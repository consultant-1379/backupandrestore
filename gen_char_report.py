#!/bin/python3
"""
Script to consume a BRO performance testdeploy.log (or any testdeploy.log that
contains Span logs) and produce an ADP characteristics report, with a usecase
per Span name. Takes the location of the testdeploy.log, the service under test
("SUT"), a "base template" which is an ADP characteristics report with an empty
"results" section (static information filled out as needed in other sections),
the name of the cluster the monitoring chart and SUT are deployed in and a regex
passed to athena to filter the pods being included in the report.

Usage:
./gen_char_report.py <testdeploy.log> <SUT deployment namespace> <base template> <cluster name> <regex>

E.g. (taken from a locally run test):
./gen_char_report.py pod_logs/testdeploy.log ebrooli-dev ci_config/characteristics-report.json hoff068 "eric-ctrl-bro-.*"

"""
import sys
import os
import json

def run(cmd, bail=True):
    print(f"RUNNING: {cmd}")
    res = os.system(cmd)
    if(res != 0):
        print(f"ERROR: Return value {res}")
        print(f"Command: {cmd}")
        if(bail):
            sys.exit(res)

def add_usecases(spans, template_path, output_path):
    with open(template_path, "r") as f:
        template = json.loads(f.read())
    template["ADP-Microservice-Characteristics-Report"]["results"] = []
    for span in spans:
        template["ADP-Microservice-Characteristics-Report"]["results"].append(
            {
                "use-case": span["name"],
                "description": span["tags"]["description"] if "description" in span["tags"] else f"TODO: description for UC {span['name']}",
                "labels": span["tags"]["labels"] if "labels" in span["tags"] else f"[]",
                "duration": 0,
                "metrics": []
            }
        )
    with open(output_path, "w") as f:
        f.write(json.dumps(template, indent=2))

def update_report(span, namespace, in_path, cluster, regex):
    phase = span["tags"]["phase"] if "phase" in span["tags"] else "idle"
    include_traffic_for_phases = ["upgrade", "rollback"]
    addon_info = '--addon-info ci_config/traffic-info.yaml' if any(traffic_phase in phase for traffic_phase in include_traffic_for_phases) else ''
    cmd = f"""athena adp_char_report \\
                --use-case={span["name"]} \\
                --pods="{regex}" \\
                -f standard \\
                -n {namespace} \\
                -log INFO \\
                -e {span["end"]} \\
                -s {span["start"]} \\
                --pm-url pm.monitor.{cluster}.rnd.gic.ericsson.se \\
                {addon_info} --char-report {in_path} -o .
    """
    run(cmd)

def default(args, i, fallback):
    if (len(args) >= i+1):
        return args[i]
    else:
        return fallback

def main():
    test_agent_log = default(sys.argv, 1, "pod_logs/testdeploy.log")
    namespace = default(sys.argv, 2, "cicd-performance")
    report_path = default(sys.argv, 3, "ci_config/characteristics-report.json")
    cluster = default(sys.argv, 4, "hall060")
    regex = default(sys.argv, 5, "eric-ctrl-bro-.*")
    with open(test_agent_log, "r") as f:
        lines = [x.strip() for x in f.readlines() if "SPAN;" in x]
    spans = [json.loads(x.split(";", 1)[-1]) for x in lines]
    # testdeploy.log includes duplicate spans, so make them unique
    spans = list({x["name"]:x for x in spans}.values())
    intermediate_report = "char-report-intermediate.json"
    add_usecases(spans, report_path, intermediate_report)
    for span in spans:
        report_path = update_report(span, namespace, intermediate_report, cluster, regex)

if __name__ == "__main__":
    main()