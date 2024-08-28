"""
Global constants common to tests.
"""
# DEFINE TEST AGENT DATA
V4_FRAG_CM_NAME = "v4-frag-and-metadata"
V4_FRAG_CM = {"AgentId": V4_FRAG_CM_NAME,
              "pod_prefix": V4_FRAG_CM_NAME + "-agent",
              "fragments": [{"fragmentId": V4_FRAG_CM_NAME + "_1",
                             "customData": True},
                            {"fragmentId": V4_FRAG_CM_NAME + "_2",
                             "customData": True}]}

V4_MULTI_FRAG_CM_NAME = "v4-fragments-some-incl-meta"
V4_MULTI_FRAG_CM = {"AgentId": V4_MULTI_FRAG_CM_NAME,
                    "fragments": [{"fragmentId": V4_MULTI_FRAG_CM_NAME + "_1",
                                   "customData": True},
                                  {"fragmentId": V4_MULTI_FRAG_CM_NAME + "_2",
                                   "customData": False},
                                  {"fragmentId": V4_MULTI_FRAG_CM_NAME + "_3",
                                   "customData": True}]}

V4TLS_FRAG_ONLY = {"AgentId": "v4tls-fragment-only",
                   "fragments": [{"fragmentId": "v4tls-fragment-only_1",
                                  "customData": False}]}

V3_FRAG_ONLY = {"AgentId": "v3-fragment-only",
                "fragments": [{"fragmentId": "v3-fragment-only_1",
                               "customData": False}]}

V3_FRAG_CM_NAME = "v3-frag-and-metadata"
V3_FRAG_CM = {"AgentId": V3_FRAG_CM_NAME,
              "fragments": [{"fragmentId": V3_FRAG_CM_NAME + "_1",
                             "customData": True},
                            {"fragmentId": V3_FRAG_CM_NAME + "_2",
                             "customData": True}]}
V2_FRAG_ONLY_NAME = "v2-fragment-only"
V2_FRAG_ONLY = {"AgentId": V2_FRAG_ONLY_NAME,
                "fragments": [{"fragmentId": V2_FRAG_ONLY_NAME + "_1",
                               "customData": False},
                              {"fragmentId": V2_FRAG_ONLY_NAME + "_2",
                               "customData": False},
                              {"fragmentId": V2_FRAG_ONLY_NAME + "_3",
                               "customData": False}]}

V2_FRAG_CM_NAME = "v2-frag-metadata"
V2_FRAG_CM = {"AgentId": V2_FRAG_CM_NAME,
              "fragments": [{"fragmentId": V2_FRAG_CM_NAME + "_1",
                             "customData": True},
                            {"fragmentId": V2_FRAG_CM_NAME + "_2",
                             "customData": True},
                            {"fragmentId": V2_FRAG_CM_NAME + "_3",
                             "customData": True},
                            {"fragmentId": V2_FRAG_CM_NAME + "_4",
                             "customData": True},
                            {"fragmentId": V2_FRAG_CM_NAME + "_5",
                             "customData": True}]}

V4_NO_FRAG_CM_ONLY = {"AgentId": "v4-metadata-only"}
V4_NO_FRAG_OR_META = {"AgentId": "v4-no-frag-or-meta"}

# v4_fragments_with_and_without_custom_metadata subscriber and
# v4_fragments_with_and_without_custom_metadata configuration-data
# are used for backup type tests

V4_MULTI_FRAG_CM_SUBSCRIBER = \
    {"AgentId": V4_MULTI_FRAG_CM_NAME,
     "fragments": [{"fragmentId": V4_MULTI_FRAG_CM_NAME + "_1",
                    "customData": True}]}

V4_MULTI_FRAG_CM_CONFIGURATION = \
    {"AgentId": V4_MULTI_FRAG_CM_NAME,
     "fragments": [{"fragmentId": V4_MULTI_FRAG_CM_NAME + "_2",
                    "customData": False}]}

LG_AGENT = {"AgentId": "agent-large-file",
            "pod_prefix": "agent-large-file-agent"}

TLS_AGENT = {"AgentId": "tls",
             "pod_prefix": "tls-agent",
             "fragments": [{"fragmentId": "tls_1", "customData": False}]}

DDPG_AGENT = {"AgentId": "eric-data-document-database-pg",
              "pod_prefix": "eric-data-document-database-pg"}

KVDB_AGENT = {"AgentId": "eric-data-kvdb-ag-admin-mgr-0",
              "pod_prefix": "eric-data-kvdb-ag"}
