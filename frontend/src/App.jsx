import React, { useState, useEffect } from 'react';
import VideoPlayer from './VideoPlayer';
import './App.css';

const BACKEND_BASE_URL = 'http://localhost:8000/api/v1';

export default function App() {
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selected, setSelected] = useState({ id: null, title: 'Select a video to play' });
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [form, setForm] = useState({ file: null, title: '', description: '' });
  const [showSnackbar, setShowSnackbar] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState('');

  useEffect(() => {
    fetchVideos();
  }, []);

  const fetchVideos = () => {
    setLoading(true);
    fetch(`${BACKEND_BASE_URL}/videos`)
      .then(res => {
        if (!res.ok) throw new Error(res.status);
        return res.json();
      })
      .then(setVideos)
      .catch(() => setError('Failed to load. Ensure backend/API & CORS are live.'))
      .finally(() => setLoading(false));
  };

  const handleUpload = (e) => {
    e.preventDefault();
    if (!form.file || !form.title) return alert('File and title are required.');

    const data = new FormData();
    data.append('file', form.file);
    data.append('title', form.title);
    data.append('description', form.description);

    setUploading(true);
    setUploadProgress(0);

    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${BACKEND_BASE_URL}/videos`);

    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) {
        setUploadProgress(Math.round((e.loaded / e.total) * 100));
      }
    };

    xhr.onload = () => {
      setUploading(false);
      setForm({ file: null, title: '', description: '' });
      fetchVideos();
      setSnackbarMessage('✅ Video uploaded successfully!');
      setShowSnackbar(true);
      setTimeout(() => setShowSnackbar(false), 3000);
    };

    xhr.onerror = () => {
      setUploading(false);
      alert('Upload failed.');
    };

    xhr.send(data);
  };

  return (
    <div className="container">
      <aside className="sidebar">
        <div className="upload-section">
          <h2 style={{textAlign: "left", marginTop:"5px"}}>🚀🚀 Upload new video</h2>
          <form className="upload-form" onSubmit={handleUpload}>
            <input
              type="file"
              accept="video/*"
              onChange={e => setForm({ ...form, file: e.target.files[0] })}
              required
            />
            <input
              type="text"
              placeholder="Title"
              value={form.title}
              onChange={e => setForm({ ...form, title: e.target.value })}
              required
            />
            <textarea
              placeholder="Description"
              value={form.description}
              onChange={e => setForm({ ...form, description: e.target.value })}
            />
            <button type="submit" disabled={uploading}>Upload</button>
            {uploading && (
              <div className="progress-bar">
                <div className="progress" style={{ width: `${uploadProgress}%` }} />
              </div>
            )}
          </form>
        </div>

        <hr />

        {loading ? (
          <p>Loading videos…</p>
        ) : error ? (
          <p className="error">⚠️ {error}</p>
        ) : videos.length === 0 ? (
          <p>No videos yet. Upload one via the form above.</p>
        ) : (
            <div style={{ flexGrow: 1, overflowY: 'auto', alignContent:'center' }}>
              <h2 style={{textAlign: "left", marginTop:"5px"}}>🎬 List of uploaded Videos</h2>
              <ul>
                {videos.map(v => (
                  <li
                    key={v.videoId}
                    className={v.videoId === selected.id ? 'active' : ''}
                    onClick={() => setSelected({ id: v.videoId, title: v.title })}
                  >
                    <p style={{"color": "#000000" }}>{v.title}</p>
                  </li>
                  
                ))}

                
              </ul>
            </div>
        )}
      </aside>

      <main className="player-section">
        {selected.id ? (
          <VideoPlayer videoId={selected.id} backendUrl={BACKEND_BASE_URL} />
        ) : (
          <div className="placeholder">Choose a video on the left to start.</div>
        )}
      </main>

      {showSnackbar && (
        <div className="snackbar">{snackbarMessage}</div>
      )}
    </div>
  );
}
