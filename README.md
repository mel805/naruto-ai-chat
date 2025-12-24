# 🍜 Naruto AI Chat

An Android chat application powered by AI (Llama 3.2) featuring characters from Naruto and famous celebrities with realistic personalities.

## ✨ Features

### 🎭 Characters

**Naruto Universe:**
- 🍜 Naruto Uzumaki - The hyperactive ninja
- ⚡ Sasuke Uchiha - The cool Uchiha prodigy
- 🌸 Sakura Haruno - The intelligent kunoichi
- 📖 Kakashi Hatake - The Copy Ninja
- 💜 Hinata Hyuga - The gentle Hyuga heiress
- 🌙 Itachi Uchiha - The tragic genius

**Male Celebrities:**
- 🎬 Brad Pitt - Hollywood icon
- 🌊 Leonardo DiCaprio - Academy Award winner
- 💪 Dwayne "The Rock" Johnson - The People's Champion

**Female Celebrities:**
- 🕷️ Scarlett Johansson - Black Widow actress
- 💎 Margot Robbie - Australian star
- 📚 Emma Watson - Hermione & activist
- ✨ Zendaya - Multi-talented icon

### 🔥 Key Features

- **SFW & NSFW Modes** - Toggle between safe and uncensored conversations
- **Realistic Personalities** - Each character has unique personality traits and speaking styles
- **Context-Aware** - Maintains conversation history for natural dialogue
- **Beautiful UI** - Modern Material Design 3 interface
- **Offline-First** - Works with your own Oracle Cloud VM

## 📱 Screenshots

*Coming soon*

## 🚀 Installation

### For Users

1. Download the latest APK from [Releases](../../releases)
2. Enable "Install from Unknown Sources" in your Android settings
3. Install the APK
4. Configure your Oracle Cloud IP (see Setup below)
5. Start chatting!

### Requirements

- Android 8.0 (API 26) or higher
- Internet connection
- Oracle Cloud VM with Llama 3.2 running (see Setup)

## ⚙️ Setup

### Oracle Cloud Setup (Free Tier)

1. **Create Oracle Cloud Account**
   - Go to https://cloud.oracle.com/free
   - Sign up (free forever)

2. **Create ARM VM (Always Free)**
   - Compute → Instances → Create Instance
   - Shape: VM.Standard.A1.Flex
   - OCPU: 4, Memory: 24 GB
   - Image: Ubuntu 22.04 ARM
   - Download SSH key

3. **Install Ollama + Llama**
   ```bash
   # SSH into your VM
   ssh -i your-key.pem ubuntu@YOUR-VM-IP
   
   # Install Ollama
   curl -fsSL https://ollama.com/install.sh | sh
   
   # Download Llama 3.2
   ollama pull llama3.2:3b  # or llama3:8b for better quality
   
   # Configure for external access
   sudo systemctl edit ollama.service
   # Add: Environment="OLLAMA_HOST=0.0.0.0:11434"
   sudo systemctl restart ollama
   ```

4. **Open Firewall**
   - Oracle Console → Networking → Security Lists
   - Add Ingress Rule: TCP port 11434, Source 0.0.0.0/0

5. **Configure App**
   - Open app
   - Go to Settings
   - Enter your Oracle VM IP: `http://YOUR-VM-IP:11434`
   - Test connection

### Alternative: Local Setup

You can also run Llama locally on your Freebox or server. See [MODELES_LEGERS_NSFW_FREEBOX.md](../MODELES_LEGERS_NSFW_FREEBOX.md) for instructions.

## 🛠️ Development

### Build from Source

```bash
git clone https://github.com/YOUR-USERNAME/naruto-ai-chat
cd naruto-ai-chat
./gradlew assembleRelease
```

### Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material Design 3
- **Architecture:** MVVM
- **AI Backend:** Llama 3.2 via Ollama API
- **HTTP Client:** OkHttp
- **Async:** Kotlin Coroutines

## 🎮 Usage

### Basic Chat

1. Select a character from the list
2. Start typing your message
3. Wait for the AI to respond (2-5 seconds)

### Switching Modes

- Click the 🔓 lock icon in the top bar
- Toggle between SFW (safe) and NSFW (uncensored)
- Character personality adapts to the mode

### Tips

- Be specific in your questions
- Build on previous messages for better context
- Each character has unique traits - explore them!

## 🔒 Privacy & Safety

- All conversations happen between your phone and your server
- No data is sent to third parties
- NSFW mode is for adults only (18+)
- You control the server - you control your data

## 📄 License

MIT License - See [LICENSE](LICENSE) for details

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## ⚠️ Disclaimer

This app is for entertainment purposes only. Characters are AI-generated and do not represent real people or their views. Use responsibly.

## 💬 Support

Having issues? Open an issue on GitHub or check the [Discussions](../../discussions) page.

## 🙏 Credits

- **Naruto** © Masashi Kishimoto
- **Llama** © Meta AI
- **Celebrities** - Personalities are AI interpretations for entertainment

---

**Built with ❤️ using Kotlin, Jetpack Compose, and Llama 3.2**

⭐ Star this repo if you like it!
