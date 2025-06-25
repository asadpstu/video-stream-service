# 🎥 Video Streaming App

A simple full-stack video upload and playback platform built with **Spring Boot** (backend) and **React + Vite** (frontend). The app allows you to upload videos, transcode using FFmpeg, and view them with a responsive video player.



## Features

- Upload video files via the frontend UI
- View and select uploaded videos
- Backend processing with FFmpeg
- Beautiful and modern React interface



### Prerequisites

Make sure you have the following installed:

- [Node.js (v18+ recommended)](https://nodejs.org/)
- [Java 17+](https://adoptium.net/)
- FFmpeg

#### Install FFmpeg on macOS

If you're using macOS, install FFmpeg using [Homebrew](https://brew.sh/):

```bash
brew install ffmpeg
ffmpeg -version
```

### 🚀 Now start your springboot app from your favourite editor.

### Start the Frontend

This is a React + Vite app. Please note, I didn't focus on frontend code quality a lot as it is a practice project.

```bash
cd frontend
npm install
npm run dev
```

- Runs on port `5173`
- Access the frontend at: [http://localhost:5173](http://localhost:5173)



## App Preview

The app UI looks like this:

<img width="1512" alt="Screenshot 2025-06-26 at 01 32 29" src="https://github.com/user-attachments/assets/75520c54-a38d-46a1-9bf5-9f2ece16c1a2" />



## Tech Stack

- **Backend:** Java, Spring Boot
- **Frontend:** React, Vite
- **Video Processing:** FFmpeg



## License

Any one can use it. Happy coding!
