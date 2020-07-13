#!/usr/bin/env kscript

@file:Include("decode_single_layer.kts")

//File("initial_instructions.txt").decodeLayer0("results/layer0.txt")
//File("results/layer0.txt").decodeLayer1("results/layer1.txt")
//File("results/layer1.txt").decodeLayer2("results/layer2.txt")
File("results/layer2.txt").decodeLayer3("results/layer3.txt")