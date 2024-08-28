### Table Of Agents and Description
| Agent Name                  | Agent Alias        | API Version | Description                                                              |
|-----------------------------|--------------------|-------------|--------------------------------------------------------------------------|
| v4-frag-and-metadata        | V4_FRAG_CM         | v4          | Agent includes two fragments and both include custom metadata            |
| v4-fragments-some-incl-meta | V4_MULTI_FRAG_CM   | v4          | Agent includes three fragments, two with custom metadata and one without |
| v4tls-fragment-only         | V4TLS_FRAG_ONLY    | v4          | Agent with one fragment and no custom metadata.                          |
| v3-frag-and-metadata        | V4_NO_FRAG_CM_ONLY | v4          | Agent - No fragment and has custom metadata only                         |
| v3-fragment-only            | V3_FRAG_ONLY       | v3          | Agent with one fragment and no custom metadata                           |
| v3-frag-and-metadata        | V3_FRAG_CM         | v3          | Agent includes two fragments and both with custom metadata               |
| v2-fragment-only            | V2_FRAG_ONLY       | v2          | Agent with three fragments and no custom metadata                        |
| v2-frag-metadata            | V2_FRAG_CM         | v2          | Agent includes five fragments and include custom metadata                |
| agent-large-file            | LG_AGENT           | v4          | Agent that contains large backup. Default is 3GB                         |
| tls                         | TLS_AGENT          | v4          | TLS agent - fragment only                                                |
| fails-restore               | None               | v4          | Agent will fail during a restore                                         |
| fails-backup                | None               | v4          | Agent will fail during a backup                                          |
| agent-pod-fails             | None               | v4          | Agent pod will fail to deploy                                            |
| agent-perf1gb               | AGENT_LARGE_FILE   | v4          | Agent with 1GB backup                                                    |
| agent-fragment              | AGENT_FRAG         | v4          | Agent that has many fragments. Default is 1000 fragments                 |
| agent-dies-restore          | None               | v4          | Agent will close the control channel on a restore.                       |
