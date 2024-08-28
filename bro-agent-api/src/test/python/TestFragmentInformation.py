from bro_agent.fragment.FragmentInformation import FragmentInformation


def test_fragment_information_cunstruction():
    fragment = FragmentInformation(
        "1",
        "1.0",
        "0",
        "CustomInfo",
    )
    assert \
        fragment.fragment_id == "1" and \
        fragment.version == "1.0" and \
        fragment.size_in_bytes == "0" and \
        fragment.custom_information == "CustomInfo"

if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
