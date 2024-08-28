class FragmentInformation:
    """ Fragment information """
    def __init__(self, fragment_id, version, size_in_bytes, custom_information):
        self.fragment_id = fragment_id
        self.version = version
        self.size_in_bytes = size_in_bytes
        self.custom_information = custom_information
