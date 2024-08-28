class FailedToTransferBackupException(Exception):
    """ Represents an error has occurred during a backup """
    pass


class InvalidRegistrationInformationException(Exception):
    """ An exception thrown by the validate() method of RegistrationInformation object """
    pass
