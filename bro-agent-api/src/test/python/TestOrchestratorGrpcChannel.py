import threading
import queue
import time
import unittest
from unittest.mock import Mock

from bro_agent.agent.OrchestratorGrpcChannel import OrchestratorGrpcChannel

class TestOrchestratorGrpcChannel:
    def test_generate_returns_an_iterable(self):
        generator = OrchestratorGrpcChannel(None)
        generator.send_message('TestMsg')
        try:
            next(generator.generate_control_message())
        except AttributeError:
            assert False
        assert True

    def test_generate_will_yield_what_sent(self):
        generator = OrchestratorGrpcChannel(None)
        generator.send_message('TestMsg')
        assert next(generator.generate_control_message()) == 'TestMsg'


    def test_generate_can_yield_multiple_times(self):
        generator = OrchestratorGrpcChannel(None)
        generator.send_message('TestMsg1')
        generator.send_message('TestMsg2')
        assert next(generator.generate_control_message()) == 'TestMsg1' and next(generator.generate_control_message()) == 'TestMsg2'


    def test_generate_yield_works_regardless_the_order_of_producing_and_consuming(self):
        generator = OrchestratorGrpcChannel(None)
        q = queue.Queue()

        def consumeMessages(q):
            result = next(generator.generate_control_message()) == 'TestMsg1' and next(generator.generate_control_message()) == 'TestMsg2'
            q.put(result)

        def produceMessages():
            generator.send_message('TestMsg1')
            generator.send_message('TestMsg2')

        consumerThread = threading.Thread(target=consumeMessages, args=(q,))
        consumerThread.start()
        produceMessages()
        consumerThread.join()

        assert q.get()

    def callback(self, message):
        self.queue.append(message)

    def returnlist(self, ret):
        yield next(ret)

    def backup_func(self, msg):
        self.queue.append(next(msg))

    def test_controlstream_one_message_to_agentcallback(self):
        generator = OrchestratorGrpcChannel(None)

        generator.control_stub = Mock()
        generator.data_stub = Mock()
        generator.control_stub.establishControlChannel = self.returnlist

        self.queue = []
        generator.set_process_callback(self.callback)

        generator.establish_control_channel()

        generator.send_message('TestMsg1')
        time.sleep(1)

        generator.shutdown()

        assert self.queue[0] == 'TestMsg1'

    def test_controlstream_one_message_to_agentcallback(self):
        generator = OrchestratorGrpcChannel(None)

        generator.control_stub = Mock()
        generator.data_stub = Mock()
        generator.data_stub.backup = self.backup_func

        self.queue = []
        generator.open_data_stream()

        class MockMessage:
            def __init__(self, message):
                self.message = message
                self.dataMessageType = 0

        generator.send_backup(MockMessage('TestMsg1'))
        time.sleep(1)

        generator.close_data_stream()

        assert self.queue[0].message == 'TestMsg1'

if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
