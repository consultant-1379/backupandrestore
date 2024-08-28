class FileChunkServiceUtil:
    """ Process the filechunks """
    def __init__(self):
        self.FILE_CHUNK_SIZE = 512000

    def process_file_chunks(self, path):
        """ Process the file chunks """
        with open(path, 'rb') as f:
            while True:
                chunk = f.read(self.FILE_CHUNK_SIZE)
                if chunk:
                    yield (chunk, len(chunk))
                else:
                    break
