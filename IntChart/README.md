# Integration Chart

## Viewing logs in Kibana (Data Visualizer KB)

### Prerequisites

* You must have our Integration Chart deployed in a Kubernetes environment

### Accessing the Kibana UI

Kibana is exposed externally using NodePort. To access the logs, you will need two things:

* The IP address of a node in the cluster
* The port which exposes Kibana to the outside world

To get the IP addresses of the nodes in the cluster, you can run the following command:

    kubectl get nodes -o wide

This command will return all of the nodes in the cluster, along with their IP addresses. You can choose the IP address of any
of the worker nodes (the nodes that do not have "master" in the ROLES column).

To get the port, run the following command:

    kubectl get services | grep eric-data-visualizer-kb

This will return output which looks something like the following:

    eric-data-visualizer-kb             NodePort    10.99.89.92      <none>        31000:31741/TCP

In this example, the port is 31741. When you run this command, the port will most likely be something different for you.

Once you have the IP address and the port, you can use your browser to go to http://<YOUR_IP_ADDRESS>:<YOUR_PORT>. This will open
the Kibana UI. 

### Viewing the logs

First you must configure an index for the logs:

* In the Kibana UI, click Discover in the menu on the left of the screen. 
* You will be prompted to create and index pattern. Enter the astrisk character (*) in the Index Pattern text-box.
* Click the "Next Step" button on the right hand side of the screen.
* In the Time Filter Field Name dropdown box, select @timestamp.
* Click the "Create Index Pattern" button on the right hand side of the page.

Once this is done, you should be able to view logs:

* Click Discover in the menu on the left of the screen. 
* All logs will be displayed.
* Use the search bar at the top of the screen to search for specific logs

## Troubleshooting

### Issues with clusterroles

When installing the integration chart, you may get the following error message:

    Error: release <NAMESPACE> failed: clusterroles.rbac.authorization.k8s.io "bro-intchart-monitoring" already exists

The problem here is that a clusterrole is created when the integration chart is installed. The name given to this clusterrole is 
always "bro-intchart-monitoring". If the integration chart has already been installed on the cluster (for example, someone else 
has installed it for testing), a clusterrole with that name will already exist. This causes the `helm install` to fail.

This means that you cannot have two Integration Chart instances running in the cluster at the same time. At the moment, this should
not be an issue as the Integration Chart should only be installed in our shared test KaaS environment by our Jenkins jobs. Our
Jenkins jobs are configured not to run in parallel if they involve installing the integration chart, so there should never be a
situation where two Integration Chart instances are installed at the same time. However, we may need to revisit this issue if that
changes.
