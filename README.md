[![Build Status](https://travis-ci.org/servicecatalog/provisioning-service.svg?branch=master)](https://travis-ci.org/servicecatalog/provisioning-service)
# provisioning-service

The provisioning-service project is a microservice for managing applications deployment and lifecycle on Kubernetes Cluster. The software to be deployed and the description of the Kubernetes resources are packaged as Helm Charts.
The provisioning microservice connects to a [Rudder REST Proxy](https://github.com/servicecatalog/rudder) deployed in the cluster, 
which interfaces the Helm Charts Repositories and the Helm Tiller Server which manages the software installation.
Popular software packages for testing can be found in the [kubernetes charts](https://github.com/kubernetes/charts) repository.
The microservice uses the [oscm microservices framework](https://github.com/servicecatalog/service-tools) framework based on Commander and Eventsouring patterns with Kafka Streams.


## Getting Started

TODO

