import { API_URL } from '../config';

class UploadService {
  constructor() {
    this.uploads = new Map();
    this.listeners = new Map();
    this.isProcessing = false;
  }

  startUpload(file, onProgress, onComplete, onError, mappingData = null, customFileName = null) {
    const uploadId = Date.now().toString() + '_' + (customFileName || file.name);
    
    const uploadData = {
      id: uploadId,
      fileName: customFileName || file.name,
      originalFileName: file.name,
      fileSize: file.size,
      status: 'UPLOADING',
      progress: 0,
      startTime: Date.now(),
      abortController: new AbortController(),
      onProgress,
      onComplete,
      onError,
      mappingData: mappingData,
      customFileName: customFileName
    };

    this.uploads.set(uploadId, uploadData);
    this.notifyListeners();
    this.processUpload(uploadId, file);
    
    return uploadId;
  }

  async processUpload(uploadId, file) {
    const upload = this.uploads.get(uploadId);
    if (!upload) return;

    try {
      const formData = new FormData();
      const fileName = upload.customFileName || file.name;
      formData.append('file', file, fileName);
      
      if (upload.mappingData) {
        formData.append('allocationMonthYear', upload.mappingData.allocationMonthYear || '');
        formData.append('lenderProcessName', upload.mappingData.lenderProcessName || '');
        formData.append('uploadName', upload.mappingData.uploadName || '');
      }

      const response = await fetch(`${API_URL}/leads/upload`, {
        method: 'POST',
        body: formData,
        signal: upload.abortController.signal,
        headers: {
          'Authorization': 'Bearer ' + (localStorage.getItem('token') || '')
        }
      });

      let progress = 0;
      const interval = setInterval(() => {
        progress += Math.random() * 15;
        if (progress >= 90) {
          clearInterval(interval);
          progress = 90;
        }
        upload.progress = Math.min(progress, 90);
        if (upload.onProgress) {
          upload.onProgress(upload.progress);
        }
        this.notifyListeners();
      }, 200);

      if (!response.ok) {
        throw new Error(`Upload failed: ${response.status}`);
      }

      clearInterval(interval);
      
      const data = await response.json();
      
      upload.progress = 100;
      upload.status = 'COMPLETED';
      upload.result = data;
      upload.endTime = Date.now();
      
      if (upload.onProgress) {
        upload.onProgress(100);
      }
      if (upload.onComplete) {
        upload.onComplete(data);
      }
      
      this.notifyListeners();
      
      setTimeout(() => {
        this.uploads.delete(uploadId);
        this.notifyListeners();
      }, 30000);

    } catch (error) {
      if (error.name === 'AbortError') {
        upload.status = 'CANCELLED';
        console.log('Upload cancelled:', uploadId);
      } else {
        upload.status = 'FAILED';
        upload.error = error.message;
        console.error('Upload error:', error);
        if (upload.onError) {
          upload.onError(error);
        }
      }
      this.notifyListeners();
    }
  }

  cancelUpload(uploadId) {
    const upload = this.uploads.get(uploadId);
    if (upload) {
      upload.abortController.abort();
      this.uploads.delete(uploadId);
      this.notifyListeners();
      return true;
    }
    return false;
  }

  getUploadStatus(uploadId) {
    const upload = this.uploads.get(uploadId);
    if (upload) {
      return {
        id: upload.id,
        fileName: upload.fileName,
        originalFileName: upload.originalFileName,
        fileSize: upload.fileSize,
        progress: upload.progress,
        status: upload.status,
        startTime: upload.startTime,
        endTime: upload.endTime,
        result: upload.result,
        error: upload.error,
        mappingData: upload.mappingData
      };
    }
    return null;
  }

  getAllUploads() {
    const result = [];
    for (const [id, upload] of this.uploads) {
      result.push({
        id: upload.id,
        fileName: upload.fileName,
        originalFileName: upload.originalFileName,
        fileSize: upload.fileSize,
        progress: upload.progress,
        status: upload.status,
        startTime: upload.startTime,
        endTime: upload.endTime,
        mappingData: upload.mappingData
      });
    }
    return result;
  }

  addListener(callback) {
    const id = Date.now().toString();
    this.listeners.set(id, callback);
    return id;
  }

  removeListener(id) {
    this.listeners.delete(id);
  }

  notifyListeners() {
    const uploads = this.getAllUploads();
    for (const callback of this.listeners.values()) {
      callback(uploads);
    }
  }

  cleanup() {
    const now = Date.now();
    for (const [id, upload] of this.uploads) {
      if (upload.status === 'COMPLETED' || upload.status === 'FAILED' || upload.status === 'CANCELLED') {
        if (now - (upload.endTime || now) > 60000) {
          this.uploads.delete(id);
        }
      }
    }
    this.notifyListeners();
  }
}

const uploadService = new UploadService();

setInterval(() => {
  uploadService.cleanup();
}, 30000);

export default uploadService;
