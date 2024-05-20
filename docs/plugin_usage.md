# Plugin usage

## Launching the plugin

To launch the plugin, open ImageJ and navigate to the `Plugins` menu. 
The plugin will be listed under `BII > Replisome Analysis`.

## Choosing an image

Specify the path to the folder containing the images you wish to analyze, then select the image you wish to analyze.


## Choosing what to run

The plugin provides two distinct analysis workflows, Colony Detection and Replisome Tracking.
You can run both or only one of these workflows by checking the boxes in the `Run` section.

## Configuration

### Using an existing configuration

For default images, some configurations are already available, these will be used if you check the relevant box.
You can then select one of these configurations from the dropdown menu.

### Specifying a new configuration

If you wish to specify a new configuration, simply uncheck the box and fill in the fields as required.

- Colony detection :
  - Minimum colony area: The minimum area of a colony in pixels.
- Tracking replisomes :
  - Radius : the mean radius of the replisome in microns.
  - Quality threshold : the minimum quality of the spots. Spots with a quality below this threshold will be discarded.
  - Median filter : whether to apply a median filter to the image before tracking the replisomes.
  - Max linking distance : the maximum distance in microns that a spot can move between frames.
  - Max gap distance : the maximum gap in microns for merging two tracks. This will join two tracks if the distance between the end of one track and the start of the next is less than this value.
  - Max frame gap : the maximum number of frames that can be skipped when linking two tracks.
  - Duration filter : the minimum duration of a track in frames. Any track shorter than this will be discarded.

### Display options

You can choose to display additional results, at the cost of more memory usage.

- Show colony regions : the Voronoi diagram used to assign spots to colonies will be displayed.