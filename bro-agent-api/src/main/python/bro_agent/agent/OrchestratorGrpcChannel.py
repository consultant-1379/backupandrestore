import grpc
import logging
import queue
import threading
import time

import bro_agent.generated.INT_BR_ORCH_CTRL_pb2_grpc as INT_BR_ORCH_CTRL
import bro_agent.generated.INT_BR_ORCH_DATA_pb2_grpc as INT_BR_ORCH_DATA
import bro_agent.generated.INT_BR_ORCH_DATA_pb2 as INT_BR_ORCH_DATA_TYPES


class OrchestratorGrpcChannel:
    MAXIMUM_NUMBER_OF_ATTEMPTS_TO_START_LISTEN = 20
    """ OrchestratorGrpcChannel """
    def __init__(self, channel=None):
        self.queue = queue.Queue()
        self.data_queue = queue.Queue()
        self.agent_alive = True
        self.data_channel_alive = False
        self.send_thread = None
        self.callback = None

        self.number_of_attempts_to_listen = 0

        self.channel_init(channel)

    def channel_init(self, channel):
        self.channel = channel
        if self.channel is not None:
            self.control_stub = INT_BR_ORCH_CTRL.ControlInterfaceStub(self.channel)
            self.data_stub = INT_BR_ORCH_DATA.DataInterfaceStub(self.channel)
        else:
            self.control_stub = None
            self.data_stub = None
        self.control_channel_alive = True

    def set_process_callback(self, callback):
        self.callback = callback

    def set_on_error(self, on_error):
        self.on_error = on_error

    def establish_control_channel(self):
        """ Establish the control channel """
        self.listen_thread = threading.Thread(target=self.__start_listen_on_control_channel)
        self.listen_thread.start()

    def generate_control_message(self):
        """ Generate control message """
        while self.control_channel_alive:
            try:
                item = self.queue.get(True, 0.1)
            except queue.Empty:
                continue
            logging.info("control message: %s", item)
            yield item
            self.queue.task_done()

    def generate_data_message(self):
        """ Generate Data Message """
        while self.data_channel_alive:
            try:
                item = self.data_queue.get(True, 0.1)
            except queue.Empty:
                continue
            yield item
            self.data_queue.task_done()

    def send_message(self, message):
        """ Send Message """
        self.queue.put(message)

    def send_backup(self, data):
        """ Send backup """
        logging.info("Sending backup to Orchestrator: {}".format(
            INT_BR_ORCH_DATA_TYPES.DataMessageType.Name(data.dataMessageType)
        ))
        self.data_queue.put(data)

    def get_restore_data_iterator(self, meta_data):
        """ Get restore data iterator """
        for data in self.data_stub.restore(meta_data):
            yield data

    def open_data_stream(self):
        """ Open data stream """
        self.data_channel_alive = True
        self.send_thread = threading.Thread(target=self.__start_backup_channel)
        self.send_thread.start()

    def close_data_stream(self):
        """ Close data stream """
        while not self.data_queue.empty():
            time.sleep(0.1)
        self.data_channel_alive = False
        self.send_thread.join()

    def close_control_stream(self):
        self.control_channel_alive = False
        if self.data_channel_alive:
            self.close_data_stream()
        for message in self.generate_control_message():
            pass
        logging.info("Channel to Orchestrator was closed")
        # Doesn't need to call this because when the queue will be empty the channel will be automatically closed
        # self.channel.close()

    def shutdown(self):
        """ Shutdown """
        self.agent_alive = False
        self.close_control_stream()
        # Shutdown MUST be called by parent thread
        self.listen_thread.join()

    def __start_backup_channel(self):
        try:
            logging.info("__start_backup_channel")
            result = self.data_stub.backup(self.generate_data_message())
            logging.info("__start_backup_channel ENDED!!")
            return result
        except Exception as e:
            logging.error("Excecption in __start_backup_channel")
            logging.error("%s", e)

    def __start_listen_on_control_channel(self):
        while self.agent_alive:
            if self.number_of_attempts_to_listen < OrchestratorGrpcChannel.MAXIMUM_NUMBER_OF_ATTEMPTS_TO_START_LISTEN:
                self.__listen_on_control_channel()
            else:
                self.agent_alive = False
            self.number_of_attempts_to_listen = self.number_of_attempts_to_listen + 1

    def channel_up_and_running(self):
        self.number_of_attempts_to_listen = 0

    def __listen_on_control_channel(self):
        try:
            for msg in self.control_stub.establishControlChannel(self.generate_control_message()):
                logging.info("Agent received a message from orchestrator: %s", msg)
                self.channel_up_and_running()
                self.callback(msg)
        except grpc.RpcError as e:
            logging.error("Channel to Orchestrator was closed due to error: %s", e)

            if e.code() == grpc.StatusCode.INVALID_ARGUMENT:
                logging.error("Not attempting to register again")
                raise e

            self.close_control_stream()
            time.sleep(2)
            self.on_error()
