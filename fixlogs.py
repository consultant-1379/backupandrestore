#!/usr/bin/env python3
"""
Reads json formatted log events, one event per line, from stdin and
reformats them to be human readable, before writing them to stdout.

Optionally supports custom configuration via json file, but default
config below is fully complete for BRO v4.6.0. Recommend contributing
any additional fields added to config while trying to parse logs from
some other services back into the default config.

It's worth noting all mandatory fields in the default config below are
mandatory according to DR-D1114-010-A,D - but of course how often
services actually follow the schema it references is anyones guess.

Note: Sometimes logs are retrieved from kubernetes as follows:
kubectl logs <pod> --timestamps

This will prepend a timestamp to the start of the log message, as follows:
2021-10-17T20:44:37.040443795Z {"version":"1.0.0", ...rest of log message...}


which will cause this script to fail to parse the messages as they are
no longer valid json. To strip these timestamps, you can use grep, as
follows:
`cat <json log file> | grep -P -o '([\s])(.*)' | ./fixlogs.py`

Example usage:
`kubectl logs --follow eric-ctrl-bro-0 | ./fixlogs.py`
`cat eric-ctrl-bro-0.log | ./fixlogs.py > fixed-eric-ctrl-bro-0.log`
`kubectl logs --follow some-other-service-pod | ./fixlogs.py some-custom-config.json`

"""
import sys
import json

DEFAULT_CONFIG = {
    "fields" : [
        {
            "keys" : ["timestamp"],
            "optional" : False
        },
        {
            "keys" : ["severity"],
            "optional" : False
        },
        {
            "keys" : ["extra_data.location.class"],
            "optional" : True,
            "ops" : [{
                "name": "splitTakeLast",
                "args" : ["."]
            }]
        },
        {
            "keys" : ["message"],
            "optional" : False
        },
        {
            "keys" : ["extra_data.exception"],
            "optional" : True
        }
    ],
    "spacer" : " ",
    "strict" : False
}

OPERATIONS = {
    "splitTakeLast" : lambda value, args: value.split(args[0])[-1]
}

def nested_get(obj, key_chunks):
    if(key_chunks[0] in obj):
        if(len(key_chunks) > 1):
            return nested_get(obj[key_chunks[0]], key_chunks[1:])
        else:
            return obj[key_chunks[0]]
    return None

def to_readable(log_event, config):
    fields = []
    for field in config["fields"]:
        value = None
        for key in field["keys"]:
            value = nested_get(log_event, key.split("."))
            if value != None:
                break
        if(value == None and not field["optional"]):
            message = f"Failed to find non optional log event field {field} in event: {log_event}"
            if(config["strict"]):
                raise RuntimeError(message)
            print(message)
            return None
        elif(value != None):
            if("ops" in field):
                for op in field["ops"]:
                    value = OPERATIONS[op["name"]](value, op["args"])
            fields.append(value)
    return config["spacer"].join(fields)

def main():
    config = DEFAULT_CONFIG
    if(len(sys.argv) > 1):
        with open(sys.argv[1], "r") as f:
            config = json.load(f)

    for line in sys.stdin:
        line = line.strip()
        try:
            event = json.loads(line, strict=False)
        except Exception as e:
            continue
        readable = to_readable(event, config)
        print(readable, flush=True)

if __name__ == "__main__":
    main()