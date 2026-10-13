# Baixar e testar

Esta pasta contém os JARs já compilados para Windows x64 e Linux x64. Eles incluem as bibliotecas nativas do respectivo sistema. Você precisa de **Java 21**, uma sessão gráfica e suporte a **OpenGL 3.3**. Não é necessário instalar Maven para executar estes arquivos.

## Windows

1. Instale um Java 21 para Windows x64.
2. Baixe `skyblock-classic-windows-0.1.0.jar`.
3. Abra PowerShell na pasta onde salvou o arquivo e execute:

```powershell
java -version
java -jar skyblock-classic-windows-0.1.0.jar
```

## Linux

1. Instale Java 21 e os drivers gráficos do seu sistema.
2. Baixe `skyblock-classic-linux-0.1.0.jar`.
3. Execute em um terminal dentro da pasta do arquivo:

```bash
java -version
java -jar skyblock-classic-linux-0.1.0.jar
```

No Arch Linux, o Java pode ser instalado com `sudo pacman -S jre21-openjdk`. A construção a partir do código exige o JDK em vez de somente JRE; veja o README principal.

## Controles

WASD para mover, mouse para olhar, espaço para pular, Shift para agachar, Ctrl para correr, clique esquerdo para quebrar, direito para usar/colocar, E para inventário, R para receitas e Esc para pausar. Abra o baú para retirar os itens iniciais. F2 salva uma captura de tela; F3 mostra informações de depuração.

O mundo é salvo automaticamente ao sair, em `.skyblock-classic/world.dat` dentro da pasta do seu usuário. Para testar com um arquivo separado:

```bash
java -jar skyblock-classic-linux-0.1.0.jar --save teste/world.dat
```

Troque pelo nome do JAR de Windows quando necessário. A opção `--no-audio` desativa som sintetizado se o dispositivo causar problemas.

## Validação e limites

Os arquivos foram compilados com Java 21. O jogo rodou no Linux com renderização por software e testes de interface. O JAR de Windows foi compilado e suas bibliotecas nativas verificadas, mas ainda não foi executado no Windows. Esta implementação não tem fidelidade completa ao Minecraft 1.0; consulte `docs/FIDELITY.md` no repositório.

`SHA256SUMS` contém os hashes dos dois arquivos. No Linux, execute `sha256sum -c SHA256SUMS`. No PowerShell, use `Get-FileHash -Algorithm SHA256 skyblock-classic-windows-0.1.0.jar` e compare o resultado.

Não há texturas, fontes, sons ou modelos originais importados. As imagens e sons do jogo são gerados pelo próprio código.
