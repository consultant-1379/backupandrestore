from bro_agent.util.ChecksumCalculator import ChecksumCalculator

def test_md5_calculation_on_empty_string():
    input_data = ""
    calculator = ChecksumCalculator()

    calculator.add_bytes(input_data.encode())

    assert calculator.get_checksum() == "d41d8cd98f00b204e9800998ecf8427e"

def test_md5_calculation_on_simple_string():
    input_data = "Hello"
    calculator = ChecksumCalculator()

    calculator.add_bytes(input_data.encode())

    assert calculator.get_checksum() == "8b1a9953c4611296a827abf8c47804d7"

def test_md5_calculation_on_multiple_string_chunks():
    input_data_first = "Hello"
    input_data_second = "World"
    calculator = ChecksumCalculator()

    calculator.add_bytes(input_data_first.encode())
    calculator.add_bytes(input_data_second.encode())

    assert calculator.get_checksum() == "68e109f0f40ca72a15e05cc22786f8e6"

def test_md5_calculation_oneliner():
    assert ChecksumCalculator().calculate_checksum("Hello") == "8b1a9953c4611296a827abf8c47804d7"

if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
