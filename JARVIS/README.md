# J.A.R.V.I.S. Mobile — projeto base (v3.0)

App Android nativo (Kotlin + Jetpack Compose) com interface Stark HUD, central de notificações holográficas,
leitura em voz alta (inclui WhatsApp) e hub de IA com vários provedores.

## Como gerar o APK (sem Android Studio)
1. Crie um repositório no GitHub e envie **todo o conteúdo desta pasta** (inclusive `.github/`).
2. Abra a aba **Actions** → *Build JARVIS APK* → **Run workflow** (ou faça um push na `main`).
3. Quando terminar, baixe o artefato **JARVIS-debug-apk** e instale o `.apk` no celular.

## Primeiro uso (aba AJUSTES)
1. **Acesso às notificações** → ativar o JARVIS. Se o Android bloquear: Configurações › Apps › JARVIS › ⋮ › *Permitir configurações restritas*.
2. **Exibir sobre outros apps** → ativar (é o que mostra os hologramas flutuantes).
3. **Sem restrição de bateria** → ativar (ajuda a manter o serviço vivo).
4. Toque em **ENVIAR HOLOGRAMA DE TESTE**.
5. Aba **IA**: ative um provedor, preencha endereço/chave, toque em **MODELOS**, escolha um e **TESTAR**.

## O que já está implementado
- Leitura de notificações de todos os apps; no WhatsApp pega remetente + texto da última mensagem.
- Hologramas flutuantes sobre qualquer app (toque abre a conversa de origem).
- Modo "substituir notificações normais": remove a notificação da barra **somente** se o holograma flutuante puder aparecer.
  Chamadas, alarmes, navegação, mídia e notificações em andamento nunca são tocados.
- Leitura em voz alta (TTS pt-BR) de notificações e mensagens do WhatsApp; respostas da IA também.
- Histórico dos últimos 100 cartões (guardado só no aparelho).
- Hub de IA: Ollama, LM Studio, DeepSeek, OpenAI, Claude, Gemini, Grok, Mistral, Groq, Perplexity, OpenRouter, Together e personalizado,
  com cadeia de fallback, busca de modelos e teste de conexão. Chaves ficam em armazenamento criptografado.

## Limites conhecidos (honestos)
- **Não foi compilado nem testado em aparelho ainda.** O primeiro build no GitHub pode apontar erros pequenos: mande o log que eu corrijo.
- O JARVIS lê **o que aparece na notificação** do WhatsApp (o app é criptografado; não há acesso ao histórico do chat).
  Mensagens com a conversa silenciada ou com prévia desativada no WhatsApp não trazem texto.
- A notificação removida não volta pra barra; o histórico fica na aba HOLO.
- Ainda não implementado (próximas fases do documento mestre): reconhecimento de voz/palavra de ativação, agente autônomo,
  acessibilidade/visão de tela, câmera, resposta direta ao WhatsApp, rotinas, dispositivos.
- O tráfego HTTP sem TLS está liberado para falar com Ollama/LM Studio na rede local. Use HTTPS para provedores de nuvem.
