@ -0,0 +1,21 @@
# Replisome Analysis : ImageJ plugin for the analysis of DNA replication dynamics in bacteria

## Welcome to the Replisome Analysis Plugin

This ImageJ plugin is intended to provide an automated analysis of DNA replication dynamics in bacteria.

### Intended Use


This plugin works for images of bacteria, specifically with a DIC channel and a fluorescent channel. The plugin is designed to analyze the dynamics of DNA replication in bacteria, specifically the movement of the replisome.

Bacteria are assigned to colonies using the DIC channel, and GFP-tagged DNaN in the second channel is used to track the replisome. The plugin can be used to track the movement of the replisome.

Various plots and statistics can then be generated from the data.

## Installation

To install the plugin, take the provided .jar file in GitHub Packages and place it in the plugins folder of your ImageJ installation.

The plugin will then be available in the ImageJ menu under Plugins > Replisome Analysis.

## Usage

For information on how to use the plugin, see the [Plugin Usage](plugin_usage.md) page.