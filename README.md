# The Paremus Service Fabric Promises and Async Services examples

This repository contains example applications for the Paremus Service Fabric. All examples 
can be built locally, or release versions are available from https://nexus.paremus.com. Instructions for running these examples on the Paremus Service Fabric may be found at https://docs.paremus.com/display/SF113/Tutorials.


All sources in this repository are provided under the Apache License Version 2.0

# The `fractal` application

This example is targetted for version 1.13.x of the Paremus Service Fabric

The application consists of:

 * An Equation bundle, which supplies calculations for Mandelbrot and Julia sets.
 * A colour map, which provides colouring options for the renderer
 * A web rendering interface, which uses JAX-RS and Server Sent Events to communicate with a JavaScript front end.

