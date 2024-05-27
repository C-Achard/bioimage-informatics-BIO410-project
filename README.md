<img src="docs/resources/logo.png" alt="Logo" width="350" align="right" vspace = "80"/>

# Replisome Analysis : ImageJ plugin for the analysis of DNA replication dynamics in bacteria

Welcome to the Replisome Analysis ImageJ plugin, designed to provide an automated analysis of DNA replication dynamics in bacteria.

Using information from differential interference contrast (DIC) microscopy for the bacterial bodies, and a fluorescent channel for the replisome, 
the plugin can analyze the dynamics of DNA replication in bacteria, specifically the movement of the replisome. 

## Hardware requirements

The plugin is designed to work with 2D+t images of bacteria.
Due to the size of the images, a computer with at least 8GB of RAM is recommended; 16GB is preferred.

**Note**:
If you consistently run out of memory, you may want to adjust the memory setting in ImageJ.
Go to Edit>Options>Memory & Threads and adjust the memory settings as needed, using no more than 75% of your available RAM.
See [this page](https://docs.openmicroscopy.org/bio-formats/5.7.1/users/imagej/managing-memory.html#increasing-imagej-fijis-memory) for more information. 

## Installation and usage documentation

### Dependencies

**First, install the following dependencies manually :**

- cilj2 : due to the GPU acceleration provided by clij, it is safer to instal it from source to ensure there are no drivers issues.
  Follow [this link](https://clij.github.io/clij2-docs/installationInFiji) to install clij2 in Fiji.
- commons-csv : download the **1.8** version from [here](https://archive.apache.org/dist/commons/csv/binaries/) and place the .jar in the `plugins/` folder of your ImageJ installation.
- xchart : download the **3.8.8** version [here](https://knowm.org/open-source/xchart/xchart-change-log/) and place the .jar in the `plugins/` folder of your ImageJ installation.

### Install from .jar file

To install the plugin, download the .jar from GitHub Packages on the right bar, and place it in the `plugins/` folder of your ImageJ installation.

You can the find the plugin in the ImageJ menu under `Plugins > Replisome Analysis`.

Full information on how to use the plugin can be found in the [Documentation](https://c-achard.github.io/bioimage-informatics-BIO410-project/).

## Data availability and specifications

The plugin is designed to work with 2D+t images of bacteria. 
This version is intended to run only on the data available [on OMERO](https://omero.epfl.ch/webclient/?show=project-2857).

Specifications:

- 2D + time, with 1s frame interval
- 60x 1.4 NA oil objective, 103 um/pixel
- 2 channels :
    - Channel 1 : Phase contrast (DIC), Bacteria morphology
    - Channel 2 : Fluorescence, replisome foci (sfGFP Fluorescent protein)

## Used libraries and tools

This plugin uses :

- [ImageJ](https://imagej.net/) : Image processing and analysis in Java
- [OMERO](https://www.openmicroscopy.org/omero/) : Open Microscopy Environment for biological imaging
- [Fiji](https://fiji.sc/) : ImageJ distribution with plugins and libraries
- [TrackMate](https://imagej.net/plugins/trackmate/) : ImageJ plugin for the analysis of single-particle tracking data
- [clij2](https://clij.github.io/) : GPU-accelerated image processing in Image
- [Maven](https://maven.apache.org/) : Software project management and comprehension tool

## Authors

- Cyril Achard
- Mathilde Morelli
- Linkai Dai

## Acknowledgements

This project was developed as part of the BIO-410 course at EPFL.
Thanks to the teachers and TAs !
