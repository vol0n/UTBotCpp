# What is UTBotCpp?

[![Build UTBot and run unit tests](https://github.com/UnitTestBot/UTBotCpp/actions/workflows/build-utbot.yml/badge.svg)](https://github.com/UnitTestBot/UTBotCpp/actions/workflows/build-utbot.yml)
[![Publish UTBot as an archive](https://github.com/UnitTestBot/UTBotCpp/actions/workflows/publish-utbot.yml/badge.svg)](https://github.com/UnitTestBot/UTBotCpp/actions/workflows/publish-utbot.yml)

UTBot C/C++ generates test cases by code, trying to cover a maximum number of statements and execution paths. We treat source code as a source of truth, assuming that behavior is correct and corresponds to initial user demand. Generated tests are placed in the so-called regression suite. Thus, we fix current behavior with the help of generated test cases. Using UTBot for C/C++, developers obtain full control of their code. No future change can break the code without being noticed once it's covered with tests generated by UTBot. This way, modifications made by developers to an existing code are much safer. Hence, with the help of generated unit tests, UTBot provides dramatic code quality improvement.

Features demonstration in 5 min (click to see on [YouTube](https://www.youtube.com/watch?v=bDJyWEeYhvk)): 

[![UTBot C/C++ Demo](https://img.youtube.com/vi/bDJyWEeYhvk/0.jpg)](https://www.youtube.com/watch?v=bDJyWEeYhvk "UTBot C/C++ Demo")

More info on [wiki](https://github.com/UnitTestBot/UTBotCpp/wiki)

## How to install and use UTBot

For now, you can only use UTBot under Ubuntu 18.04 and above.
Navigate to the [**Releases**](https://github.com/UnitTestBot/UTBotCpp/releases) GitHub page and download last version of UTBot.

UTBot is distrbuted as an archive that contains:

1. A pack `utbot_distr.tar.gz` that contains UTBot binary and its dependencies;
2. UTBot plugin for Visual Studio code — `utbot_plugin.vsix`;
3. A version `version.txt`;
4. A run script `unpack_and_run_utbot.sh`.

To launch UTBot, `unzip` the archive and run the 
`./unpack_and_run_utbot.sh` command (we recommend doing it in a fresh directory to make UTBot removing easier). To remove UTBot, simply delete this directory.

To install UTBot VSCode plugin, use VSCode *Install from VSIX* command.

## How to contribute to UTBot

See [**Contributing guidelines**](CONTRIBUTING.md) and [**Developer guide**](DEVNOTE.md)
