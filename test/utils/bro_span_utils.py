#!/usr/bin/env python3
"""
This module is used to create SPAN logs which are used to track
the duration of a certain operation.
"""

import json
import time


class Span:
    """
    Span: used to track how long something takes, and produce a
    string designed to be easy to filter log files for containing
    a name, start time, end time and set of tags
    Log looks like:
    SPAN;{"name":"<name>","start":<time>,"end":<time>,"tags":<map>}
    """
    def __init__(self, name, tags=None):
        if tags is None:
            tags = {}
        self.name = name
        self.start = int(time.time())
        self.tags = tags
        self.end = 0

    def finish(self):
        """
        Ends a span
        """
        self.end = int(time.time())
        return "SPAN;" + self.get_log()

    def get_log(self):
        """
        Gets a json representation of a span object
        """
        span = {
            "name": self.name,
            "start": self.start,
            "end": self.end,
            "tags": self.tags
        }
        return json.dumps(span)
