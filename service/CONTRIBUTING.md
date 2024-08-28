# Contributing to Backup and Restore Orchestrator

This document describes how to contribute artifacts for the **Backup and Restore Orchestrator** service.

When contributing to this repository, please first discuss the change you wish to make via [JIRA issue][features], email, or any other method with the [guardians](#Project-Guardians) of this repository before making a change.

The following is a set of guidelines for contributing to **Backup and Restore Orchestrator** project. These are mostly guidelines, not rules. Use your best judgment, and
feel free to contact the guardians if you have any questions.

[TOC]

## Code of Conduct

This project and everyone participating in it is governed by the [ADP Code of Conduct](https://gerrit-gamma.gic.ericsson.se/plugins/gitiles/AIA/microservices/backupandrestore/+/master/service/CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## Project Guardians

The guardians are the maintainers of this project. Their responsibility is to guide the contributors and review the submitted patches.

- Michael Lawless
- Stephen Tobin S
- Raymond Self

## Development Environment prerequisites

The development work of Backup and Restore is done in Java. A test framework, based in Python, is used to test the service. To be able to run the code and tests within the project, the following tools need to exist on the host:

- Java 11
- Python 3

## How can I use this repository?

This repository contains the source code of the Backup and Restore service including functional and test code, documentation and configuration files for manual and automatic build and verification.

If you want to fix a bug or just want to experiment with adding a feature, you should try the service in your environment using a local copy of the project's source.

You can start by cloning the GIT repository to get your local copy:

```text
git clone ssh://<userid>@gerrit-gamma.gic.ericsson.se:29418/AIA/microservices/backupandrestore
```

Once you have your local copy, you should add the submodule for the test framework:

```text
git submodule add ../testframework testframework
```

You should then update the submodules needed to build the project:

```text
git submodule update --init --recursive
```

If you are satisfied with your change and want to submit for review, create a new git commit and then push it following the [guidelines for contributors](#What-Is-Expected-In-A-Contribution).

## How Can I Contribute?

### Reporting Bugs

This section guides you through submitting a bug report for Backup and Restore Orchestrator.
Following these guidelines helps maintainers and the community understand your report, reproduce the behavior, and find related reports.

Before creating bug reports, please check [this list](#Before-Submitting-A-Bug-Report) as you might find out that you don't need to create one.
When you are creating a bug report, please [include as many details as possible](#How-Do-I-Submit-A-Bug-Report).

> **Note:** If you find a **Closed** issue that seems like it is the same thing that you're experiencing, open a new issue and include a link to the closed issue in the body of your new one.

#### Before Submitting A Bug Report

- **Check the [Troubleshooting Section of the Service User Guide][TSLink].** You might be able to find the cause of the problem and fix things yourself.
- **Perform a search in [JIRA][backlog]** to see if the problem has already been reported. If it has **and the issue is still open**, add a comment to the existing issue instead of opening a new one.
- **Perform a search in the [discussion forum][forum]** for the project to see if that bug was discussed before. If not, **consider starting a new thread** to get quick preliminary feedback from the project maintainers.

#### How Do I Submit A Bug Report?

Bugs are tracked as [JIRA issues][bugs]. Select the correct component and create an issue on it providing the following information.

Explain the problem and include additional details to help maintainers reproduce the problem:

- **Use a clear and descriptive title** for the issue to identify the problem.
- **Describe the exact steps which reproduce the problem** in as many details as possible.
- **Include one problem per Jira issue**. If you are experiencing multiple unrelated problems, open a seperate issue for each.
- **Include details about your configuration and environment**.
- **Describe the behavior you observed** and point out what exactly is the problem with that behavior.
- **Explain which behavior you expected to see instead and why.**
- **If the problem wasn't triggered by a specific action**, describe what you were doing before the problem happened.
- **Include log files from the pods**. See the Data Collection section in the [Troubleshooting Section of the Service User Guide][TSLink] for details on how to collect the log information.

### Suggesting Features

This section guides you through submitting an enhancement suggestion, including completely new features and minor improvements to existing functionality.
Following these guidelines helps maintainers and the community understand your suggestion and find related suggestions.

When you are creating a feature suggestion, please [include as many details as possible](#How-Do-I-Submit-A-Feature-Suggestion).

#### Before Submitting A Feature Suggestion

- **Check the [Service Overview][ServiceUserGuide]** for tips - you might discover that the feature is already available.
- **Perform a search in [JIRA][features]** to see if the feature has already been suggested. If it has, add a comment to the existing issue instead of opening a new one.
- **Perform a search in the [discussion forum][forum]** for the project to see if that enhancement was discussed before.

If you cannot find any information about the feature, **contact the project guardians** to see if the feature is planned. A meeting can then be setup with the project guardians to discuss the feature suggestion.

#### How Do I Submit A Feature Suggestion?

Feature suggestions are tracked as [JIRA issues][features]. Select the correct component and create an issue on it providing the following information:

- **Use a clear and descriptive title** for the issue to identify the suggestion.
- **Provide a step-by-step description of the suggested feature** in as many details as possible.
- **Explain why this feature would be useful** to other users of the service.

### Submitting Contributions

This section guides you through submitting your own contribution, including bug fixes, new features or any kind of improvement on the content of the repository.
The process described here has several goals:

- Maintain the project's quality
- Fix problems that are important to users
- Engage the community in working toward the best possible solution
- Enable a sustainable system for the project's maintainers to review contributions

#### Before Submitting A Contribution

- **Engage the project maintainers** in the proper way so that they are prepared to receive your contribution and can provide valuable suggestions on the design choices. Follow the guidelines to [report a bug](#Reporting-Bugs) or to [propose an enhancement](#Suggesting-Features).

#### What Is Expected In A Contribution?

Please follow these steps to have your contribution considered by the maintainers:

- **Follow the [styleguides](#Styleguides)** when implementing your change.
- **Provide a proper description of the change and the reason for it**, referring to the associated JIRA issue if it exists.
- **Provide clean code and unit tests for the contribution**.
- **Provide system and cluster tests to verify your change**, extending the existing test suites or creating new ones in case of new features.
  The tests suites are a Python community based framework. For more information, please refer to the [test framework documentation][TestframeworkDocumentation].
- **Update the project documentation if needed**. In case of new features, they shall be properly described in the relevant documentation.
- **Contact the service guardians if you are introducing a 3PP**, see the [Introduction of 3rd Party Software](#Introduction-of-3rd-Party-Software) section for more information.
- When **pushing your contribution for review**, the naming format should be <d_featureID> e.g. d_ADPPRG-111.
- After you submit your contribution, **verify that the automatic [CI pipeline](#CI-Pipeline) for your change is passing**. If the CI pipeline is failing, and you believe that the failure is unrelated to your change, please contact the project maintainers. A maintainer will re-run the pipeline for you.

While the prerequisites above must be satisfied prior to having your pull request reviewed, the reviewer(s) may ask you to complete additional design work, tests, or other changes before your change request can be ultimately accepted.

#### Contribution Workflow

This section outlines the process contributions must go through when they are submitted for review.

1. The **contributor** updates the artifact in their local repository on a development branch following the naming format <d_featureID> e.g. d_ADPPRG-111.
1. The **contributor** pushes the update to Gerrit for review on the development branch.
1. The **contributor** invites the **service guardian** (mandatory) and **other relevant parties** (optional) to the Gerrit review and makes no further changes to the artifact until it is reviewed.
1. The **service guardian** reviews the artifact and gives a code-review score.
The code-review scores and corresponding workflow activities are as follows:
    - Score is +1
        A **reviewer** is happy with the changes, but approval is required from another reviewer.
    - Score is +2
        The **service guardian** accepts the change.
    - Score is -1 or -2
        The **service guardian** and the **contributor** align to determine when and how the change is published.
1. When all reviews related to the featureID have been reviewed and merged to the development branch, the **service guardian** makes the decision of merging the development branch with master.

## CI Pipeline
Pushing changes for review to this project (like git push origin HEAD:refs/for/d_ADPPRG-111) will automatically trigger the [Pre-Code-Review][CIJob] CI job.
This will run all the Backup and Restore Orchestrator unit and system tests, as well as Sonarqube static analysis. All these stages must pass before the code will be reviewed.
It is the responsibility of the **contributor** to write any new test cases required to verify the code changes.
Once the contribution has been reviewed and given a +2, it can be merged.

This will trigger a second CI job. This job will run verification on the Kubernetes cluster using the below test modules:

* [nose_auto.py](https://gerrit-gamma.gic.ericsson.se/gitweb?p=AIA/microservices/backupandrestore.git;a=blob;f=test/nose_auto.py)
* [sys_test.py](https://gerrit-gamma.gic.ericsson.se/gitweb?p=AIA/microservices/backupandrestore.git;a=blob;f=test/sys_test.py)

The **contributor** must determine if the code change has any impact on these test cases and confirm with the **service guardian**.

## Introduction of 3rd Party Software
If a new 3rd party library is introduced as part of a contribution, the **contributor** must make the Backup and Restore Orchestrator team aware of this.
The **contributor** must contact the **service guardian** and provide the details of the new library. The details that must be provided are:

* The name of the library
* The CAX number of the library (which can be found in [Bazaar](https://bazaar.internal.ericsson.com))

If the library does not exist in [Bazaar](https://bazaar.internal.ericsson.com), it is the responsibility of the **contributor** to submit a Generic FOSS request to Bazaar to add the library.

## Styleguides

### Git Commit Messages

Please use the following format:
```text
<Jira Ticket Number><Jira Ticket Title>

<Short description of the change>
<Link to the Jira Ticket>

@innersource
```

Include the text @innersource in the commit message before pushing an inner source contribution for review.
Project Guardians will only accept inner source contributions if they include the text @innersource in the commit message.

### Project Best Practices

* Avoid trailing spaces in files.
* Unless there is a good reason, naming conventions should be consistent.
* Clarity is preferred over abbreviations, acronyms or codenames.
* Wildcard imports should be avoided. For clarity and to avoid conflicts, listing only necessary classes is preferred.

### Policy on Utility Classes
* New utility classes should be avoided unless there is a strong need that can be demonstrated, or it improves clarity/robustness.

### Policy on new Java features
* Use of newer Java features should be avoided until it is discussed with the team.

[bugs]: https://eteamproject.internal.ericsson.com/issues/?filter=136291
[backlog]: https://eteamproject.internal.ericsson.com/issues/?filter=145969
[features]: https://eteamproject.internal.ericsson.com/secure/CreateIssue.jspa?pid=31101&issuetype=10203
[TSLink]: https://adp.ericsson.se/marketplace/backup-and-restore-orchestrator/documentation/development/dpi/service-user-guide#troubleshooting
[ServiceUserGuide]: https://adp.ericsson.se/marketplace/backup-and-restore-orchestrator/documentation/development/dpi/service-user-guide
[forum]: https://teams.microsoft.com/l/channel/19%3a637f12cb91f34bfe9e8b9851389ef14a%40thread.skype/General?groupId=01349e19-bf00-425b-83a0-2d1510006efd&tenantId=92e84ceb-fbfd-47ab-be52-080c6b87953f
[CIJob]: https://fem41s11-eiffel004.eiffel.gic.ericsson.se:8443/jenkins/view/Plan%20B/job/BRO_Service_PCR/
[TestframeworkDocumentation]: https://gerrit-gamma.gic.ericsson.se/plugins/gitiles/AIA/microservices/testframework/
