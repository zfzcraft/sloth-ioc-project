# Sloth IOC Design Philosophy

[![zh-CN](https://img.shields.io/badge/lang-中文-blue)](README.zh-CN.md)

Sloth IOC is a new-generation ultra-lightweight IOC container deeply optimized and innovated based on the cloud-native design philosophy of Pure IOC. It inherits Pure IOC’s core positioning of a minimal kernel, transparent controllability, K8s adaptability, high concurrency and low latency, while fundamentally breaking through the underlying design limitations of traditional IOC and Pure IOC. With the core goals of **zero redundancy, extreme startup speed, and maximum lightweighting**, it is built as a more streamlined and faster-starting IOC container foundation than Pure IOC for cloud-native microservices, container elastic scheduling, and high-concurrency scenarios. It adheres to the core principles of no black boxes, non-intrusiveness, and low memory footprint, retaining only the most essential Bean management and dependency injection capabilities of an IOC container.

## I. Core Architecture Innovations (Key Differences from Pure IOC)
### 1. Complete Abandonment of BeanDefinition, Further Streamlined Underlying Architecture
In contrast to Pure IOC, which retains a minimal BeanDefinition metadata model, Sloth IOC **completely removes the BeanDefinition component**. It eliminates the entire process of Bean metadata parsing, storage, and management, with no Bean metadata processing whatsoever. The underlying architecture is simpler than Pure IOC, with no redundant objects occupying memory or complex metadata parsing logic, and no runtime metadata overhead. This fundamentally eliminates memory waste and logical redundancy, further upgrading the purity of the container kernel.

### 2. Single Container Architecture, Minimalist Without Nesting (Inherited Advantage from Pure IOC)
There is only one global ApplicationContext, with no parent-child containers, no abstract layering, and no multi-level container nesting. The architecture is extremely simple, stable, transparent, easy to maintain, and troubleshoot, fully adapting to operation, maintenance, and debugging requirements in cloud-native environments while maintaining the singularity and controllability of the container structure.

### 3. Global Singleton Scope, Cloud-Native Stateless Standard (Inherited Advantage from Pure IOC)
Following the stateless design principle of microservices, only the singleton scope is supported. All redundant scope designs are removed to avoid invalid object creation, ensuring extremely low memory usage, stable and efficient runtime, and aligning with the stateless service requirements of K8s elastic scaling.

## II. Startup Mechanism Innovations (Core Differentiating Highlights)
### 1. Extreme Lazy Loading, Startup Speed Independent of Bean Count
Abandoning Pure IOC’s model of default lazy loading + eager loading of core components + asynchronous preheating, Sloth IOC adopts a **global extreme pure lazy loading** design. During container startup, **only classes annotated with @Configuration are loaded**; all other business classes, component classes, and service classes are not loaded, parsed, or instantiated. This completely decouples startup speed from Bean count. Whether a project contains 100 or 10,000 Beans, the container startup time remains consistently **millisecond-level**, perfectly meeting K8s’ core demands for fast scheduling and second-level scaling.

### 2. No Multi-Threaded Parallel Class Loading, Simpler Startup Logic
Unlike Pure IOC, which uses multi-threaded parallel class loading to improve startup efficiency, Sloth IOC only loads configuration classes and thus requires no multi-threaded parallel loading logic. The startup process has no extra thread scheduling overhead, is simpler and more stable, further reducing startup time and achieving true zero-wait startup.

### 3. Zero Runtime Package Scanning, Index-Based Initialization (Inherited Advantage from Pure IOC)
Inefficient runtime package scanning is completely discarded. Beans are defined via a unified configuration file, and Bean indexes are automatically generated at the packaging stage. At runtime, initialization is performed directly by reading indexes with zero performance loss throughout the process, fully covering the framework core and third-party plugins with no runtime scanning overhead.

## III. Dependency Injection and Instantiation Design
### 1. Class as the Unique Identifier, Type-Safe and Unambiguous (Inherited Advantage from Pure IOC)
Using Class as the unique Bean identifier, it abandons ambiguous byName and byType matching. For single-implementation interfaces, the interface Class is used; for multi-implementation interfaces, the implementation Class is used. Full type safety, no conflicts, and no configuration ambiguity ensure precise dependency injection.

### 2. Pure Constructor Injection, No Lifecycle Callbacks (Inherited Advantage from Pure IOC)
Only constructor-based instantiation and dependency injection are supported. Field injection and Setter injection are completely abandoned, with no initialization, callbacks, or lifecycle binding. It fully complies with native Java specifications, remaining clean and minimalist with no implicit logic for in-depth debugging.

## IV. Extension Capability Design
### 1. Streamlined General Bean Extension Points, User-Defined Customization
Only the most basic Bean extension mechanism is retained, further streamlined compared to Pure IOC’s BeanPostProcessor extension point. The framework contains no built-in enhancement logic; capabilities such as AOP, dynamic proxy, transactions, and logging interception are all implemented by users based on basic extension points, fully open with no solidified logic to maintain kernel purity.

### 2. Environment Configuration Extension Capabilities (Inherited Advantage from Pure IOC)
Provides a standard EnvironmentLoader extension point to support loading environment configurations from local and remote configuration centers, natively adapting to cloud-native configuration systems. It also supports the EnvironmentPostProcessor post-configuration processing extension for custom enhancements such as encryption, desensitization, property replacement, and merging to meet enterprise-level configuration requirements.

### 3. JDK Native SPI, Plug-in Automated Assembly (Inherited Advantage from Pure IOC)
Implements plugin discovery, conditional loading, and automated assembly based on JDK’s native SPI mechanism with no third-party dependencies, ensuring stability, universality, and lightweighting while enabling plug-in capabilities without increasing container redundancy.

## V. Runtime Optimization Design
### 1. Extreme Runtime Memory Minimization
After container initialization, only configuration class information and the singleton Bean instance repository are retained. With complete abandonment of BeanDefinition, no temporary objects need to be destroyed, resulting in far lower runtime memory usage than Pure IOC. Resource consumption reaches an industry extreme, greatly reducing K8s cluster resource scheduling pressure.

### 2. Graceful Shutdown, No Redundant Destruction Ordering
Natively supports graceful shutdown with no complex and meaningless Bean destruction order design. Each object independently manages resource release, remaining minimalist, practical, and free of over-engineering for efficient and smooth shutdown.

### 3. Strict Responsibility Boundaries, Highly Pure Kernel
The IOC kernel only handles Bean management, constructor injection, and instance creation. Capabilities such as transactions, events, and monitoring are all implemented through external extensions without kernel coupling, ensuring long-term maintainability and evolvability in line with minimalist design philosophy.

## VI. Configuration Specifications and Recommended Runtime Environments
### 1. Unified Configuration File Format
Only YAML format configuration files are supported globally, with no compatibility for other redundant formats. Following minimalist design principles, it unifies specifications, reduces complexity, and improves parsing efficiency.

### 2. Core Recommended Runtime Environment
Adapting to Pure IOC’s cloud-native high-concurrency environment standards, the official minimum recommended runtime environment is JDK 21 or above, deeply embracing modern JVM features:
- JDK Version: JDK 21+ (based on virtual threads, ZGC, and other modern features)
- Garbage Collector: ZGC (low pause, high throughput, maximizing cloud-native concurrency)
- Concurrency Model: Virtual Threads, lightweight concurrency for high-concurrency scenarios

### 3. Best Practices for Memory Configuration
Relying on Sloth IOC’s ultra-lightweight, BeanDefinition-free, and extremely lazy-loaded design, an even smaller heap memory configuration of **1GB–2GB** is recommended to support high-concurrency, large-scale business scenarios. Small heaps combined with ZGC’s low-latency collection and high-concurrency virtual threads enable high-concurrency processing at extremely low resource costs.

## VII. Core Design Summary
Sloth IOC inherits Pure IOC’s essence of cloud-native, minimalist, and high-performance design. With three core innovations—**complete abandonment of BeanDefinition, loading only configuration classes at startup, and global pure lazy loading**—it achieves the extreme effect of **startup speed completely independent of Bean count and consistent millisecond-level startup**. Lighter, faster, and simpler than Pure IOC, it is the optimal IOC container choice for cloud-native microservices and high-concurrency, low-latency scenarios pursuing extreme startup efficiency and minimal resource usage.
