import hashlib


class ChecksumCalculator:
    """ Calculate the checksum """
    def __init__(self):
        self.hash = None
        # Because BRO service expects MD5 checksums then
        # we need to suppress security checks here
        self.md5 = hashlib.md5()  # nosec

    def add_bytes(self, byte_array):
        """ Add bytes """
        self.md5.update(byte_array)

    @staticmethod
    def calculate_checksum(content):
        """ Calculate the checksum """
        # Because BRO service expects MD5 checksums then
        # we need to suppress security checks here
        return hashlib.md5(content.encode('utf-8')).hexdigest()  # nosec

    def get_checksum(self):
        """ Get the checksum """
        return self.md5.hexdigest()
