#!/usr/bin/env bash

# USAGE: ./install-athena-prereqs <desired namespace e.g. characteristics-monitor> <cluster e.g. hall060, hoff068>

cluster="$1"
namespace="monitor"
basedir="$(pwd)"

cleanup() {
    cd $basedir
    rm -rf install-athena-prereqs
}

# Clean up from previous runs
cleanup

# Clone the repo
mkdir -p install-athena-prereqs
cd install-athena-prereqs
git clone ssh://gerrit-gamma.gic.ericsson.se:29418/adp-ra/adp-ra-rob-and-char

# Create the namespace if it doesn't exist
kubectl create namespace $namespace --dry-run=client -o yaml | kubectl apply -f -

# Uninstall and re-install the chart
cd adp-ra-rob-and-char/chart/
helm uninstall "athena-cluster-monitor" -n $namespace
helm install "athena-cluster-monitor" eric-rob-and-char-monitor -n $namespace --set ingress.cluster=$cluster --debug > "$basedir/monitor-install.yaml"

# Clean up after install
cleanup