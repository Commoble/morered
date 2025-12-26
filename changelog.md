# 21.11.2
* Add missing jar manifest attributes (fixes version not displaying in mods list)

# 21.11.1
* Fix compatibility with JEI versions (JEI 27.0.x and 27.3.x should both work now)
* Fix alternator model rendering lower than other logic gates in firstperson hand
* Fix placement previews for extractors and alternators missing axles
* Fix alternator placement preview having wrong colors making it look like both front and back would be powered

# 21.11.0
* Updated to MC 1.21.11 / Neoforge 21.11.12-beta
* Placement preview toggle and opacity config settings are now in preview_placement-client.toml instead of morered-client.toml

# 21.10.0
* Updated to MC 1.21.10 / Neoforge 21.10.6-beta
* If an old world is migrated from 1.21.8 to this version, some data will be lost due to the following data changes:
  * Filter, Osmosis Filter, Multifilter, and Stonemill block entities now save their inventory under "inventory" -> "stacks"

# 21.8.0
* Updated to 1.21.8
* Updated to use exmachina 0.13.0.3-beta
* Added new mechanical blocks:
  * Axles transfer mechanical power
  * Clutches transfer mechanical power but only while powered by redstone. Pliers can be used to rotate them after placing.
  * Gears transfer mechanical power, multiple gears can exist in the same block space
  * Gearshifters can either double rotation speed (but make things twice as hard to turn) or halve rotation speed (but make things half as hard to turn). Pliers can be used to rotate them after placing (hold sprint to change which way they rotate)
  * Windcatchers generate power when placed in high open spaces
* Added blocks which make use of mechanical power:
  * Alternators are plate blocks which emit redstone power in one of four output directions, rotating between them when rotated by machines
  * Extractors have been reworked. They now require mechanical power, and extract an entire itemstack from their input side whenever they make a full rotation. Pliers can be used to rotate them after placing (hold sprint to change which way they rotate)
  * Stonemills continually produce cobblestone while mechanically powered. They must also have a supply of lava and water touching their grates to function. They require a lot of torque; using gearshifters will reduce the minimum amount of torque needed, though they won't produce cobblestone as fast.
* Added Bitwise Multiplexer
* Logic Gates and wire/cable posts and junctions are now waterloggable and will no longer be broken by flowing water
* When placing logic plates or gearshifters, sprint can now be held to change which side of the block-being-placed faces the aimed-at face
* Added a search bar to the soldering table
* Added a datamap file at `data/morered/data_maps/dimension_type/wind.json` which specifies how much wind windcatchers can have in different dimensions. Currently there are valid formats, both of which can be seen in the default file:
```
{
	"values": {
		"#morered:overworld_wind": {
			"min_y": 63,
			"max_y": 150
		},
		"#morered:end_wind": {
			"value": 2
		}
	}
}
```
  * Dimensions whose types are not specified in the datamap have no wind
* Added two dimension_type tags:
  * #morered:overworld_wind (defaults to overworld)
  * #morered:end_wind (defaults to end)
* Fix hexidecrubrometer 0 texture being slightly different than the others
* Increased client config default preview placement opacity from 0.4 to 0.5 to make bitwise gate glyphs more visible
* Added some advancements

# 1.21.3-7.1.0.0
* Updated to exmachina 0.8.0.2-beta
* Bitwise logic gates now actually apply bitwise operators instead of applying boolean operators 16 times, which should make them slightly more efficient

# 1.21.2-7.0.0.0
* THis update is not compatible with existing saves from prior versions
* Fundamentally changed how wires wire to each other. This has these ramifications:
  * Wires no longer cause cascading block updates to update themselves (except where wires are connected to redstone dust)
  * Instead, the power of a set of wires is calculated, and then all connected wires update at once
  * This affects red alloy wires, colored and bundled network cables, and wire/cable posts
  * Wires, cables, and posts no longer lose power with distance; a wire network emits power equal to what it receives
    * Exception: One unit of power is lost when receiving power from redstone dust
  * There is now a maximum number of wire nodes which can be in a single wire network (default 1024, configurable in common config)
    * Each attached face of a wire block counts as one node, each channel used in a bundled cable block also counts as a node
  * Colored network cables no longer receive power from the block face they are attached to (they still emit weak power to it)
  * Redwire Post Relay Plates no longer emit or receive strong power on their orthagonal sides
  * Bundled cables and their relay plates will now generally connect to any block that a colored cable can
    * If a bundled cable receives a full redstone signal, it will use all 16 channels
  * Bitwise plates can now be connected directly to red alloy wires (this counts as receiving power on all channels)
  * Wire post blockentity ids and data have been renamed and reworked, there are now two blockentity types used by wire posts:
    * morered:wire_post (used by redwire_post, redwire_relay, redwire_junction)
    * morered:bundled_cable_post (used by bundle_relay, bundle_junction)
  * Bundled cable post blockentities no longer store power data
* Renamed some blocks to reduce word count (and their respective block ids):
  * Redwire Post Plate -> Redwire Relay (redwire_post_plate -> redwire_relay)
  * Redwire Post Relay Plate -> Redwire Junction (redwire_post_relay_plate -> redwire_junction
  * Bundled Cable Post -> Cable Relay (bundled_cable_post -> cable_relay)
  * Bundled Cable Relay Plate -> Cable Junction (bundled_cable_relay_plate -> cable_junction)
  * Bundled Network Cable -> Bundled Cable (bundled_network_cable -> bundled_cable)
  * {Color} Network Cable -> {Color} Cable ({color}_network_cable -> {color}_cable)
    * e.g. Orange Network Cable is now simply called Orange Cable
* Block ids of two-input AND/NAND gate have changed to match their localized names:
  * and_2_gate -> two_input_and_gate
  * nand_2_gate -> two_input_nand_Gate
* Red Alloy Ingots are now only craftable from copper ingots (no longer craftable from iron ingots)
* Updated Bundled Network Cable texture
* Added all the item-transportation content from the Tubes Reloaded mod, which is now More Red content instead:
  * Tubes
  * Colored Tubes
  * Redstone Tubes
  * Shunts
  * Loaders
  * Extractors
  * Filters
  * Osmosis Filters
  * Multifilters
  * Distributors
  * Pliers  
* Using the pliers (or wrenches from other mods) on logic gate blocks now rotates them (no longer rotatable with sticks)
* "morered:rotate_tints" model loader now specifies a delegate model in a "model" field instead of in the root json object
* APIs for wirability are refactored and now part of the exmachina library (More Red no longer has any java APIs itself)
* Temporarily disabled JEI/EMI compatibility for soldering recipes as recipe viewers aren't ported yet
  
# 1.21.1-6.0.0.3
* Fix soldering recipes rendering wrong in JEI. Fixes #40

# 1.21-6.0.0.2
* Fix multiplexer and pulse gate recipes requiring the wrong ingredients
* Allow red alloy wires to connect to redstone torches, detector rails, and lightning rods if they share an attachment face, and trapped chests

# 1.21-6.0.0.1
* Fixed soldering tables, hexidecrubrometers, and wire and cable posts not mining faster with pickaxe

# 1.21-6.0.0.0
* Updated to minecraft 1.21 / neoforge 21.0.40-beta. This update is not compatible with old worlds; loading old worlds may cause data loss.
* Jumbo Furnace is no longer bundled with this mod, it is now optional again. If jumbo furnace is not installed, Red Alloy Ingots will be craftable at a crafting table.

# 1.20.1-4.0.0.4
* Fixed redwire posts not emitting strong signals through solid blocks

# 1.20.1-4.0.0.3
* Updated embedded Jumbo Furnace jar to 4.0.0.5 (fixes unknown recipe category log warning)

# 1.20.1-4.0.0.2
* Updated embedded Jumbo Furnace jar to 4.0.0.4 (fixes jumbo smelting creating the wrong number of items)

# 1.20.1-4.0.0.1
* Updated embedded Jumbo Furnace jar to 4.0.0.3 (fixes jumbo furnace voiding items when full)

# 1.20.1-4.0.0.0
* Updated to 1.20.1 (requires forge build 47.1.0+). Old worlds using More Red are generally not compatible with this update.

## New Features
* Added the Pulse Gate, a block that generates a 2-tick pulse after receiving an input signal and does not produce a signal again until you turn it off and on again.
* Wire blocks and wire posts are now compatible with structure pieces; they can be rotated and mirrored, and their connected positions will generally respect this when saved and loaded by structure blocks in different orientations.

## Data Changes
* The gatecrafting_plinth block has been renamed to soldering_table. The gatecrafting recipe type has been renamed to soldering. Gatecrafting recipe files have been renamed accordingly.
* The "morered:red_alloy_ingot_with_jumbo_furnace" recipe has been renamed to "morered:red_alloy_ingot_from_jumbo_smelting"
* The "morered:red_alloy_ingot_without_jumbo_furnace" recipe has been removed
* Added a "transform" blockstate property to wire blocks and wire posts. This has eight values: `identity`, `rot_180_face_xz`, `rot_90_y_neg`, `rot_90_y_pos`, `invert_x`, `invert_z`, `swap_xz`, and `swap_neg_xz`. Blocks placed by players will normally have the 'identity' state, this state is only changed when the block is rotated/mirrored (e.g. by structure blocks).
* Wire posts' remote connection positions are now stored as relative positions. If the block has an altered transform state, the saved positions will be normalized to the 'identity' transform (and un-normalized when loaded). Wire blocks' per-side power storage is similar transformed.

## Other Changes
* Jumbo Furnace is now bundled with More Red via forge jar-in-jar


1.19.2-3.0.0.2
 * Reenable JEI support. Oops!

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