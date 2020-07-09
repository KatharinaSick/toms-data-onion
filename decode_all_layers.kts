#!/usr/bin/env kscript

@file:Include("decode_layers.kts")

//File("initial_payload.txt").decodeLayer0("results/layer0.txt")
//File("results/layer0.txt").decodeLayer1("results/layer1.txt")
File("results/layer1.txt").decodeLayer2("results/layer2.txt")