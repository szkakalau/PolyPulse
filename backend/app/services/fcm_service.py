import firebase_admin
from firebase_admin import credentials, messaging
import logging
import os

logger = logging.getLogger(__name__)

class FCMService:
    _instance = None
    _initialized = False

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(FCMService, cls).__new__(cls)
        return cls._instance

    def __init__(self):
        if not self._initialized:
            self._initialize_firebase()
            self._initialized = True

    def _initialize_firebase(self):
        try:
            # Check if credential file exists
            cred_path = "serviceAccountKey.json"
            if os.path.exists(cred_path):
                cred = credentials.Certificate(cred_path)
                firebase_admin.initialize_app(cred)
                logger.info("Firebase Admin initialized successfully")
            else:
                logger.warning(f"Firebase credential file not found at {cred_path}. FCM disabled.")
        except Exception as e:
            logger.error(f"Failed to initialize Firebase: {e}")

    def send_to_topic(self, topic: str, title: str, body: str):
        if not firebase_admin._apps:
            logger.debug("Firebase not initialized, skipping notification send.")
            return

        try:
            message = messaging.Message(
                notification=messaging.Notification(
                    title=title,
                    body=body,
                ),
                topic=topic,
            )
            response = messaging.send(message)
            logger.info(f"Successfully sent message: {response}")
        except Exception as e:
            logger.error(f"Error sending message: {e}")
