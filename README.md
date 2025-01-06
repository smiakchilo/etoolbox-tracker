# EToolbox Performance Tracker
![Project logo](all/src/main/content/META-INF/vault/definition/thumbnail.png)

![Version](https://img.shields.io/badge/version-1.0.0.SNAPSHOT-blue)
![Platform](https://img.shields.io/badge/AEM-6.5+-orange)
![License](https://img.shields.io/badge/license-Apache%202.0-green)

***

EToolbox Performance Tracker (or _EToolbox Tracker_ for brevity) is a simple tool that displays a breakdown of AEM page load times component-wise. Generally speaking, this is a UI addition to [Sling request progress tracker](https://developer.adobe.com/experience-manager/reference-materials/6-5/javadoc/org/apache/sling/api/request/RequestProgressTracker.html). However, it also provides some tooling for more granular measurement. With _EToolbox Tracker_ you can dig into individual Sling models or POJO classes.  

### Features

- Display a breakdown of page components load times as a tree;
- Retrieve timings per individual render units (such as HTL templates), Sling models, POJO classes, and even separate post-construct methods;
- Filter out small timings by an adjustable threshold;
- Highlight the slowest components;
- Install and unsinstall additional performance traps on the fly.

### System requirements

_EToolbox Tracker_ has been tested with AEM 6.5.12 and newer. It requires Java 11 runtime.

### Installation and usage

Install the _etoolbox-tracker.all-\<version>.zip_ package with Package Manager. Then navigate to [http://<your-aem-instance>:4502/etoolbox/tracker.html](http://localhost:4502/etoolbox/tracker.html). Paste the path to the page you want to analyze in the address bar, and press \<Enter> or click the `🗘` button. 

### Licensing

This software is licensed under the [Apache License, Version 2.0](./LICENSE).