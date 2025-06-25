import React, { useRef, useEffect, useState } from 'react';
import Hls from 'hls.js';

const styles = {
  video: {
    width: '100%',
    height: '550px',
    backgroundColor: '#000',
    borderRadius: '4px',
    objectFit: 'cover',
  },
  qualitySelector: {
    padding: '6px 10px',
    borderRadius: '5px',
    border: '1px solid #ccc',
    fontSize: '0.9em',
    cursor: 'pointer',
    float: 'right',
    marginBottom: '10px',
    backgroundColor: '#fff',
    width: '200px'
  },
  overlay: {
    position: 'absolute',
    top: 0, left: 0, right: 0, bottom: 0,
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    borderRadius: '8px',
    zIndex: 10,
  },
  spinner: {
    width: '40px',
    height: '40px',
    border: '4px solid #ccc',
    borderTop: '4px solid #536dfe',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },
  footer: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: '#536dfe',
    textAlign: "center",
    padding: '.5rem',
    color: "#FFF"
  },
};

const spinnerKeyframes = `
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
`;
const styleSheet = document.styleSheets[0];
if (styleSheet && ![...styleSheet.cssRules].some(rule => rule.name === 'spin')) {
  styleSheet.insertRule(spinnerKeyframes, styleSheet.cssRules.length);
}

function VideoPlayer({ videoId }) {
  const videoRef = useRef(null);
  const hlsRef = useRef(null);
  const [qualityLevels, setQualityLevels] = useState([]);
  const [currentQuality, setCurrentQuality] = useState(-1);
  const [loading, setLoading] = useState(true);

  const loadVideo = (videoSrc) => {
    const videoElement = videoRef.current;
    setLoading(true);

    setTimeout(() => {
      if (Hls.isSupported()) {
        if (hlsRef.current) hlsRef.current.destroy();
        const hls = new Hls();
        hlsRef.current = hls;
        hls.attachMedia(videoElement);
        hls.loadSource(videoSrc);

        hls.on(Hls.Events.MANIFEST_PARSED, (_, data) => {
          setQualityLevels(data.levels);
          setCurrentQuality(hls.currentLevel);
          videoElement.play().finally(() => setLoading(false));
        });

        hls.on(Hls.Events.LEVEL_SWITCHED, (_, data) => {
          setCurrentQuality(data.level);
        });

        hls.on(Hls.Events.ERROR, (_, data) => {
          if (data.fatal) {
            if (data.type === Hls.ErrorTypes.NETWORK_ERROR) hls.startLoad();
            else if (data.type === Hls.ErrorTypes.MEDIA_ERROR) hls.recoverMediaError();
            else hls.destroy();
          }
        });
      } else if (videoElement.canPlayType('application/vnd.apple.mpegurl')) {
        videoElement.src = videoSrc;
        videoElement.addEventListener('loadedmetadata', () => {
          videoElement.play();
          setLoading(false);
        });
      }
    }, 2000);
  };

  useEffect(() => {
    const videoElement = videoRef.current;
    const videoSrc = `http://localhost:8000/api/v1/videos/${videoId}/master.m3u8`;
    loadVideo(videoSrc);

    const handleEnded = () => {
      videoElement.currentTime = 0;
      videoElement.pause();
    };
    videoElement.addEventListener('ended', handleEnded);

    return () => {
      if (hlsRef.current) hlsRef.current.destroy();
      videoElement.removeEventListener('ended', handleEnded);
    };
  }, [videoId]);

  const handleQualityChange = (e) => {
    const level = parseInt(e.target.value, 10);
    const currentTime = videoRef.current.currentTime;

    if (hlsRef.current) {
      setLoading(true);
      videoRef.current.pause();
      hlsRef.current.currentLevel = level;
      setCurrentQuality(level);

      setTimeout(() => {
        videoRef.current.currentTime = currentTime;
        setLoading(false);
        videoRef.current.play();
      }, 2000);
    }
  };

  return (
    <div style={{ position: 'relative', height: '100%', paddingBottom: '20px' }}>
      <div style={{ padding: '1rem' }}>
            
                <div className="resolution-bar">
                <select
                    id="qualitySelect"
                    onChange={handleQualityChange}
                    value={currentQuality}
                    style={styles.qualitySelector}
                >
                    <option value="-1">Auto</option>
                    {qualityLevels.length > 0 && qualityLevels.map((level, index) => (
                    <option key={index} value={index}>
                        {level.height ? `${level.height}p` : `Level ${index}`} ({Math.round(level.bitrate / 1000)} kbps)
                    </option>
                    ))}
                </select>
                </div>
            

            {loading && (
                <div style={styles.overlay}>
                <div style={styles.spinner} />
                </div>
            )}

            <video ref={videoRef} controls style={styles.video} />

      </div>

      <div style={styles.footer} >
         NightCrawler Video Platform · Powered by Spring & React 
      </div>
    </div>
  );
}

export default VideoPlayer;
