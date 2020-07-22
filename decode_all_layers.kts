#!/usr/bin/env kscript

@file:Include("layer0.kts")
@file:Include("layer1.kts")
@file:Include("layer2.kts")
@file:Include("layer3.kts")
@file:Include("layer4.kts")
@file:Include("layer5.kts")
@file:Include("layer6.kts")

File("initial_instructions.txt").decodeLayer0("results/layer0.txt")
File("results/layer0.txt").decodeLayer1("results/layer1.txt")
File("results/layer1.txt").decodeLayer2("results/layer2.txt")
File("results/layer2.txt").decodeLayer3("results/layer3.txt")
File("results/layer3.txt").decodeLayer4("results/layer4.txt")
File("results/layer4.txt").decodeLayer5("results/layer5.txt")
File("results/layer5.txt").decodeLayer6("results/layer6.txt")