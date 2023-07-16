# 1.20.1-4.0.0.0
* Updated to 1.20.1 (requires forge build 47.1.0+). Old worlds using More Red are generally not compatible with this update.

## New Features
* Added the Pulse Gate, a block that generates a 2-tick pulse after receiving an input signal and does not produce a signal again until you turn it off and on again.
* Wire blocks and wire posts are now compatible with structure pieces; they can be rotated and mirrored, and their connected positions will generally respect this when saved and loaded by structure blocks in different orientations.

## Data Changes
* The gatecrafting_plinth block has been renamed to soldering_table. The gatecrafting recipe type has been renamed to soldering. Gatecrafting recipe files have been renamed accordingly.
* Jumbo Furnace is now bundled with More Red via forge jar-in-jar
* The "morered:red_alloy_ingot_with_jumbo_furnace" recipe has been renamed to "morered:red_alloy_ingot_from_jumbo_smelting"
* The "morered:red_alloy_ingot_without_jumbo_furnace" recipe has been removed
* Added a "transform" blockstate property to wire blocks and wire posts. This has eight values: `identity`, `rot_180_face_xz`, `rot_90_y_neg`, `rot_90_y_pos`, `invert_x`, `invert_z`, `swap_xz`, and `swap_neg_xz`. Blocks placed by players will normally have the 'identity' state, this state is only changed when the block is rotated/mirrored (e.g. by structure blocks).
* Wire posts' remote connection positions are now stored as relative positions. If the block has an altered transform state, the saved positions will be normalized to the 'identity' transform (and un-normalized when loaded). Wire blocks' per-side power storage is similar transformed.

## Other Changes


1.19.2-3.0.0.1
 * Fix crash when placing unplaceable blocks near blockless wires. Fixes issue #31

1.19.2-3.0.0.0
 * Ported to 1.19.2!
 * Bundled cable texture slightly less ugly (still ugly)
 * Temporarily removed Bag of Yurting compatibility with wires and cables; their data format will be refactored to be more structure-friendly in a later version
 

1.16.5-2.1.1.0
 * Added Bitwise Diodes, Bitwise NOT Gates, Bitwise AND Gates, Bitwise OR Gates, Bitwise XOR Gates, and Bitwise XNOR Gates
 * Fixed a bug where bundled cables would load incorrectly, causing them to have incorrect internal data after being loaded until their internal state updated

1.16.5-2.1.0.3
 * Added support for the Bag of Yurting mod; wires, cables, and wire/cable posts can now be correctly moved by Bags of Yurting

1.16.5-2.1.0.2
 * Fixed a bug where logic plates didn't correctly cause block updates when conducting output signals through solid cubes
 * Fixed a potential multithreading crash when registering screens during modloading

1.16.5-2.1.0.1
 * Red Alloy Wires can now connect to levers, buttons, and pressure plates
 * Fixed a bug where red alloy wires weren't causing block updates correctly for certain blocks (mostly pistons)

1.16.5-2.1.0.0
	Added red alloy wires, colored network cables, bundled cables, bundled cable posts, bundled cable relay plates, and the bundled cable spool
	Added "morered:redwire_posts" and "morered:bundled_cable_posts" block tags; added relevant blocks to these tags
	Added item tags for red alloy wires, network cables, colored network cables, bundled network cables, and all the wools
	Added "morered:set_wire_count" loot function for use with wire blocks
	Added "morered:wireparts" and "morered:rotate_tints" model loaders for use with wire block models
	Stone Plates can now be mined faster with a pickaxe but require a pickaxe to mine (logic plates can still be broken instantly)
	Redwire Spools' recipe now uses Red Alloy Wires as crafting material instead of Red Alloy Ingots
	Stopped redstone dust from forming connections to logic plates and redwire posts on sides where the block has no input or output
	Fixed bug where redwire posts and redwire post relay plates weren't conducting power through solid blocks as intended
	Fixed bug where the Hexidecrubrometer wasn't updating itself correctly after being placed when its placement changed the state of neighboring blocks
	Fixed potential issues caused by positions being stored incorrectly
	Fixed incorrect description of previewPlacementOpacity parameter in client config

1.16.4-2.0.1.3
	Added zh_cn translations by Zorc

1.16.4-2.0.1.2
	Upgrade to 1.16.4 and Forge Build 35.1.0

1.16.3-2.0.1.1
	Fixed potential crashes and other problems that could occur when attempting to place some blocks in places where the block can't be placed

1.16.3-2.0.1.0
	Fixed a bug that significantly impacted server performance whenever players began tracking chunks
	Fixed a bug that caused the data used for rendering redwire wires to linger in memory when logging out of one server and logging into another
	Red Alloy Ingots are now part of the "forge:ingots/redstone_alloy" item tag used by several other mods; More Red's recipes have changed to use this tag. The old "forge:ingots/red_alloy" tag remains, but should be considered deprecated in favor of the other tag

1.16.3-2.0.0.0
	Added Hexidecrubrometer
	Renamed Redwire Posts to Redwire Post Relay Plates
	Added Redwire Posts (without plates) and Redwire Post Plates (without connections to neighbors)
	Trying to place a block where a redstone wire post wire is is now denied on the client in addition to the server (this will help prevent "phantom blocks" in high-latency situations)
	Added a new ingredient type: "morered:tag_stack"
	More Red recipes now use tag ingredients instead of specific items in more places:
	More Red recipes that previously used redstone dust now use the forge:dusts/redstone tag
	More Red recipes that previously used stone plates now use the forge:quarter_slabs/smooth_stone tag
	More Red recipes that previously used smooth stone slabs now use the forge:slabs/smooth_stone tag
	More Red recipes that previously used smooth stone blocks now use the forge:smooth_stone tag
	Added stone plates to the forge:quarter_slabs/smooth_stone tag
	The default datapack included with the mod adds smooth stone and smooth stone slabs to the respective tags mentioned above as well

1.15.2-1.0.1.0
	Added Redwire Posts, Redwire Spools, and Red Alloy Ingots
	Added two-input AND and NAND gates from porl! Many thanks to porl.

1.0.0.0
	added most logic plate blocks and the gatecrafting plinth