from unittest.mock import Mock
from unittest.mock import patch
from unittest.mock import mock_open

from bro_agent.filetransfer.FileChunkServiceUtil import FileChunkServiceUtil

@patch("builtins.open", mock_open(read_data=b"Data1"))
def test_reading_one_chunk():
    file_chunk_service_util = FileChunkServiceUtil()
    file_chunk_service_util.FILE_CHUNK_SIZE = 5

    chunk, chunk_size = next(file_chunk_service_util.process_file_chunks("/testfile"))

    assert \
        chunk == b"Data1" and \
        chunk_size == 5


def test_reading_more_than_one_chunk():
    file_chunk_service_util = FileChunkServiceUtil()
    file_chunk_service_util.FILE_CHUNK_SIZE = 5

    with patch("builtins.open", mock_open(read_data=b"Data1")):
        chunk, chunk_size = next(file_chunk_service_util.process_file_chunks("/testfile"))

    assert \
        chunk == b"Data1" and \
        chunk_size == 5

    with patch("builtins.open", mock_open(read_data=b"Data")):
        chunk, chunk_size = next(file_chunk_service_util.process_file_chunks("/testfile"))

    assert \
        chunk == b"Data" and \
        chunk_size == 4

if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
