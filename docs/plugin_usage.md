# Plugin usage

:material-cog: This page provides information on how to use the Replisome Analysis plugin, namely available parameters and display options. 

## Launching the plugin

To launch the plugin, open ImageJ and navigate to the `Plugins` menu. 
The plugin will be listed under `BII > Replisome Analysis`.

[//]: # (TODO : add image of the plugin menu in ImageJ, with interface) 

## Choosing an image

Specify the path to the folder containing the images you wish to analyze, then select the image you wish to analyze.


## Choosing what to run

The plugin provides two distinct but related analyses, Colony Detection and Replisome Tracking.
You can run both or only one of these workflows by checking the boxes in the `Run` section.

!!! note
    The plugin will run the selected workflows in the order they are listed, so if you wish to run both, make sure to check both boxes.
    Running the analysis requires that both tracking and colony detection have been run previously.

## Configuration

### Using an existing configuration

For default images, some configurations are already available, these will be used if you check the relevant box.
You can then select one of these configurations from the dropdown menu.

!!! warning
    The configuration files are stored in resources. The automated loading may sometimes fail, in which case you will need to specify the configuration manually.
    You may find the parameters for the project images [on GitHub](https://github.com/C-Achard/bioimage-informatics-BIO410-project/tree/main/src/main/resources/configs)

Default configurations are available for :
- *Merged-1.tif*
- *Merged-2.tif*
- *Merged-3.tif*

### Specifying a new configuration

If you wish to specify your own configuration, or no configuration is available, simply uncheck the box and fill in the fields as required.

#### Colony detection

- **Minimum colony area** : The minimum area of a colony in pixels.

#### Tracking replisomes

- **Radius** : the mean radius of the replisome in microns.
- **Quality threshold** : the minimum quality of the spots. Spots with a quality below this threshold will be discarded.
- **Median filter** : whether to apply a median filter to the image before tracking the replisomes.
- **Max linking distance** : the maximum distance in microns that a spot can move between frames.
- **Max gap distance** : the maximum gap in microns for merging two tracks. This will join two tracks if the distance between the end of one track and the start of the next is less than this value.
- **Max frame gap** : the maximum number of frames that can be skipped when linking two tracks.
- **Duration filter** : the minimum duration of a track in frames. Any track shorter than this will be discarded.

#### Display options

You can choose to display additional results, at the cost of more memory usage.

- **Show colony regions** : the Voronoi diagram used to assign spots to colonies will be displayed.

![Example of colony detection](resources/images/voronoi.gif)
*Example of the Voronoi diagram used for colony detection (animated over time)*

[//]: # (<p align="center">)

[//]: # (  <video src="resources/videos/voronoi.gif" alt="Example of colony detection" width="500" controls/>)

[//]: # (  <br>)

[//]: # (    <i>Example of Voronoi diagram from the colony detection</i>)