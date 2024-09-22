# Apache HTTP 5 prometheus interceptor

This is an example of how to create metrics to observe request latency using the old prometheus Java SDK (0.6.0) and the Apache HTTP 5 client. 


Ressources : 
- https://docs.micrometer.io/micrometer/reference/reference/httpcomponents.html
- https://github.com/micrometer-metrics/micrometer/blob/main/micrometer-core/src/main/java/io/micrometer/core/instrument/binder/httpcomponents/hc5/ObservationExecChainHandler.java