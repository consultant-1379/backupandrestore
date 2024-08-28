from bro_agent.fragment.BackupFragmentInformation import BackupFragmentInformation


def test_backup_fragment_information_cunstruction():
    fragment = BackupFragmentInformation(
        "1",
        "1.0",
        "0",
        "CustomInfo",
        "/backup/path",
        "/custom/backup/path",
    )
    assert \
        fragment.fragment_id == "1" and \
        fragment.version == "1.0" and \
        fragment.size_in_bytes == "0" and \
        fragment.custom_information == "CustomInfo" and \
        fragment.backup_path == "/backup/path" and \
        fragment.custom_metadata_path == "/custom/backup/path"

if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
