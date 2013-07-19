Chroot-plugin
=============

The aim of this jenkins plugin is to provide support for some chroot
environment technologies.  The first two environments to implement are pbuilder
and mock. This way a jenkins job can use it's own isolated chroot environment,
can install custom repositories and dependencies, and when the job is done, the
chroot environment ist thrown away. This feature makes jenkins much more
attractive as a build server for C and C++ projects.

Pbuilder works quite nice already. Mock is not useable at the moment.

Installation from source
========================

```bash
git clone https://github.com/rmohr/chroot-plugin
cd chroot-plugin
mvn hpi:package
```

Using pbuilder
==============

To allow jenkins to use pbuilder it is necessary to that the jenkins user can
run /usr/sbin/pbuilder via sudo. Make sure to protect your jenkins installation
properly, because pbuilder is NOT a secure  and fully isolated
environment.

Using the plugin
================

 * Create chroot environments in _Manage Jenkins_ > _Chroot Environments_
 * A buildstep _chroot builder_ is now available where you can select a preconfigured builder.
 
TODO

Future Plans
============
As the prove of concept phase is over, the next step is to clean the code and
create a clean interface for long term stability.
