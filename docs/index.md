# Replisome Analysis : ImageJ plugin for the analysis of DNA replication dynamics in bacteria

## Welcome to the Replisome Analysis Plugin

:material-hand-wave: This ImageJ plugin is intended to provide an automated analysis of DNA replication dynamics in bacteria.

## Intended Use

This plugin works for 2D+t images of bacteria, specifically via differential interference contrast (DIC) microscopy for the bacterial bodies, and a fluorescent channel. 
The plugin is designed to analyze the dynamics of DNA replication in bacteria, specifically the movement of the replisome.

[//]: # (![Example of DIC channel]&#40;resources/images/DIC_example.png&#41;)

[//]: # (*Example of the bacteria in the DIC channel*)

<p align="center">
  <img src="resources/images/DIC_example.png" alt="Example of the bacteria in the DIC channel" width="500"/>
  <br>
  <i>Example of the bacteria in the DIC channel</i>
</p>


Bacteria are assigned to colonies using the DIC channel, and GFP-tagged DNaN in the second channel is used to track the replisome. The plugin can be used to track the movement of the replisome.

[//]: # (![Example of GFP channel]&#40;resources/images/GFP_example.png&#41;)

[//]: # (*Example of the replisome foci, tagged with GFP*)

<p align="center">
  <img src="resources/images/GFP_example.png" alt="Example of the bacteria in the GFP channel" width="500"/>
  <br>
  <i>Example of the bacteria in the GFP channel</i>
</p>

Various plots and statistics can then be generated from the data.

## Installation

To install the plugin, take the provided .jar file in GitHub Packages and place it in the plugins folder of your ImageJ installation.

The plugin will then be available in the ImageJ menu under Plugins > Replisome Analysis.

## Data availability

The plugin is designed to work with 2D+t images of bacteria.
This version is intended to run only on the data available [on OMERO](https://omero.epfl.ch/webclient/?show=project-2857).

The images used in this plugin are :

- *Merged-1.tif*
- *Merged-2.tif*
- *Merged-3.tif*

## Usage

For information on how to use the plugin, see the [Plugin Usage](plugin_usage.md) page.