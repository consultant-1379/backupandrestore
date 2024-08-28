#!/usr/bin/env python3
"""
This module provides methods to verify the integration of
document-database-pg with the Orchestrator.
"""

import os
import base64
import random
import asyncio
import asyncpg
from asyncpg import exceptions as apg_exc
import k8sclient

DB_CON = None

KUBE = k8sclient.KubernetesClient()
NAMESPACE = os.environ.get('kubernetes_namespace')
POSTGRES_SERVICE = 'eric-data-document-database-pg'
POSTGRES_SECRET = 'docdb-secret'

CONNECT_TIMEOUT = 3

# Global SQL Statements used on the tests
SQL_CREATE_TABLE = 'CREATE TABLE ECI_PCI( eci VARCHAR(50),' + \
    ' pci VARCHAR(50) NOT NULL ) '
SQL_SELECT_COUNT = "SELECT COUNT(*) FROM ECI_PCI WHERE eci='%s';"
SQL_SELECT_100 = "SELECT * FROM ECI_PCI WHERE eci='%s' LIMIT 100;"
SQL_INSERT_DATA = 'INSERT INTO ECI_PCI (eci, pci) VALUES ($1,$2);'
SQL_DELETE_DATA = "DELETE FROM ECI_PCI WHERE eci='%s';"
SQL_DROP_TABLE = 'DROP TABLE ECI_PCI'

# Indicates the number of records to insert
SQL_NUM_RECORDS = 10

# Keyword used in tests to insert and delete data
SQL_KEYWORD_TEST = '1'

# Global variables used in the connection
HOST = ''
USER = 'postgres'
PASSWORD = ''
PORT = ''


def decode_base64(b64_str):
    """
    Decode a string on radix-64
    Args:
        b64_str: String coded on Radix-64
    Return: A string decoded
    """
    return base64.b64decode(b64_str).decode("utf-8", "ignore")


def rnd_generator(size=6):
    """
    Generate Random strings
    Args:
        size: Integer indicating the number of characters required for
              the random word
    Return: Random string generated
    """
    your_letters = "abcdefghijklmnopqrstuvwxyz"
    return ''.join((random.choice(your_letters) for i in range(size)))


def _try_connect(**kwargs):
    """
    Tries to open a simple DB connection
    Args:
        kwargs: A list of strings required for the connection
    Returns:
        An asyncpg connection
    """
    return asyncio.get_event_loop().run_until_complete(
        asyncpg.connect(**kwargs))


def verify_connection(settings):
    """
    Initialize the connection parameters and verify the connection
    Args:
        settings: A list of parameters required for the connection
                  positional order:
                    [0] Host
                    [1] User
                    [2] Password
                    [3] Port
    Exception:
        On connection exception print the message
    """
    test_host = settings[0]
    test_user = settings[1]
    test_pwd = settings[2]
    test_port = settings[3]
    try:
        conn = asyncio.get_event_loop().run_until_complete(
            asyncpg.connect(host=test_host,
                            port=test_port,
                            user=test_user,
                            password=test_pwd))
    except apg_exc.InternalClientError as error:
        print(error)
    finally:
        asyncio.get_event_loop().run_until_complete(
            conn.close())


def connect():
    """
    Connects to a postgres db.
    Initialize the global variable DB_CON with an asyncpg connection
    Uses the global parameters initialized on setup()
    """
    global DB_CON
    DB_CON = asyncio.get_event_loop().run_until_complete(
        asyncpg.connect(host=HOST,
                        port=PORT,
                        user=USER,
                        password=PASSWORD))


def setup():
    """
    Initializes the settings required for postgres connections
    Tests and Initializes a connection with the DB
    On Error print and raises an Exception
    """
    global HOST, USER, PASSWORD, PORT
    try:
        HOST = POSTGRES_SERVICE
        secret_data = KUBE.get_namespace_secrets(
            NAMESPACE, [POSTGRES_SECRET])
        pg_settings = secret_data[0].to_dict()
        PASSWORD = base64.b64decode(
            pg_settings["data"]["super-pwd"]).decode("utf-8", "ignore")
        PORT = 5432
        settings = HOST, USER, PASSWORD, PORT
        verify_connection(settings)
        connect()

    except Exception as error:
        print(error)
        raise Exception("cannot initialize the connection") from error


def teardown():
    """
    Close the connection with the DB
    """
    asyncio.get_event_loop().run_until_complete(
        DB_CON.close())


def create_table():
    """
    Creates a temporary table
    Exception
        On exception print the error
    """
    try:
        asyncio.get_event_loop().run_until_complete(
            DB_CON.execute(SQL_CREATE_TABLE))
    except apg_exc.InternalClientError as error:
        print(error)
        raise Exception("Cannot create a temporary table ") from error


def drop_table():
    """
    Remove the temporary table
    Exception
        On exception print the error
    """
    try:
        asyncio.get_event_loop().run_until_complete(
            DB_CON.execute(SQL_DROP_TABLE))
    except apg_exc.InternalClientError as error:
        print(error)


def get_number_records(eci):
    """
    Retrieves the number of records for a particular id
    Args:
        eci: String used as an id to fetch the number of records
    Returns
        Integer indicating the number of records found
    """
    values = asyncio.get_event_loop().run_until_complete(
        DB_CON.fetchval(SQL_SELECT_COUNT % eci))
    return values


def delete_data(eci):
    """
    delete data from the temporary table
    Args:
        eci: String used as an id to delete data
    """
    asyncio.get_event_loop().run_until_complete(
        DB_CON.fetch(SQL_DELETE_DATA % eci))


def createdb(temp_db):
    """
    Creates a temporary database for tests
    Args:
        temp_db: String indicating temporary database name
    Exception
        On exception print the error
    """
    try:
        asyncio.get_event_loop().run_until_complete(
            DB_CON.execute('CREATE DATABASE %s;' % temp_db))
    except apg_exc.InternalClientError as error:
        print(error)
        raise Exception("Cannot create a temporary database ") from error


def dropdb(temp_db):
    """
    drop a temporary database
    Args:
        temp_db: String indicating temporary database name to be dropped
    """
    asyncio.get_event_loop().run_until_complete(
        asyncio.gather(DB_CON.execute(
            'DROP DATABASE IF EXISTS %s;' % temp_db)))


def get_all_data(eci):
    """
    Return a list of tuples with the information from the select
    Args:
        eci: String used as an id to retrieve all data
             Maximum number of records are 100
    """
    mobile_records = asyncio.get_event_loop().run_until_complete(
        DB_CON.fetch(SQL_SELECT_100 % eci))
    return mobile_records


def pg_insert_data(number):
    """
    Insert a number of random records
    Args:
        number: Integer representing the number of rows to insert
    Exception
        On exception print the error
    """
    try:
        insert_stmt = asyncio.get_event_loop().run_until_complete(
            DB_CON.prepare(SQL_INSERT_DATA))
        # Generates random data for index 1
        for _ in range(number):
            asyncio.get_event_loop().run_until_complete(
                insert_stmt.fetch(SQL_KEYWORD_TEST, rnd_generator(5)))
    except apg_exc.InternalClientError as error:
        print("inserting data %s" % error)


def validate_db(expected_value):
    """
    Validates the number of records in the DB
    Args:
        number: Integer representing the number of rows to insert
    """
    num_records = get_number_records(SQL_KEYWORD_TEST)
    assert num_records == expected_value, \
        "Not all expected records were retrieved"


def setup_pg_db(num_records):
    """
    Prepares the backup creating
        temporary database
        temporary table
        inserting SQL_NUM_RECORDS records with random data
    """
    global SQL_NUM_RECORDS
    SQL_NUM_RECORDS = num_records
    setup()
    createdb('test_bro')
    create_table()
    pg_insert_data(SQL_NUM_RECORDS)


def clean_pg_db():
    """
    Delete all data previuosly added using the key 1
    This method is called once backup is performed
    """
    delete_data(SQL_KEYWORD_TEST)


def teardown_pg_db():
    """
    Clean the elements used in the database
    """
    drop_table()
    dropdb('test_bro')
    teardown()
