# Project_Nexa
Frontend- Angular, Backend-Spring Boot

📊 Aging Report — কী দেখাবে?
কোন Customer এর কত দিনের পুরনো বকেয়া
বয়স অনুযায়ী ভাগ করা
উদাহরণ
Customer       | Current | 1-30 days | 31-60 days | 60+ days | Total Due
ABC Trading    | 0       | 19,000    | 0          | 0        | 19,000
XYZ Company    | 5,000   | 0         | 10,000     | 0        | 15,000
Current     → এখনো due date পার হয়নি
1-30 days   → 1-30 দিন late
31-60 days  → 31-60 দিন late
60+ days    → 60+ দিন late


১. Email Service (SMTP)
২. Email Verification
   - User তৈরি হলে verification email পাঠাবে
   - Link click করলে account active হবে

③. Forgot Password
   - Email দিলে reset link পাঠাবে
   - Link click করলে নতুন password দেওয়া যাবে


📋 Database Tables
Email Verification
email_verifications
id
userId
token             → UUID (unique)
expiresAt         → 24 hours পরে expire
verified          → false (default)
createdAt
Password Reset
password_reset_tokens
id
userId
token             → UUID (unique)
expiresAt         → 1 hour পরে expire
used              → false (default)
createdAt

🔄 Flow
Email Verification Flow
Admin creates User
        ↓
System sends verification email
  Link: http://localhost:4200/verify-email?token=xxx
        ↓
User clicks link
        ↓
POST /api/auth/verify-email?token=xxx
        ↓
emailVerified = true
status = ACTIVE
Forgot Password Flow
User clicks "Forgot Password"
        ↓
POST /api/auth/forgot-password { email }
        ↓
System sends reset email
  Link: http://localhost:4200/reset-password?token=xxx
        ↓
User clicks link → enters new password
        ↓
POST /api/auth/reset-password { token, newPassword }
        ↓
Password updated
All refresh tokens revoked

🌐 API Endpoints
POST /api/auth/verify-email?token=xxx
POST /api/auth/forgot-password   { email }
POST /api/auth/reset-password    { token, newPassword }
POST /api/auth/resend-verification { email }

📁 Folder Structure
com.nexaerp/
│
├── email/
│   ├── EmailService.java
│   ├── EmailServiceImpl.java
│   └── dto/
│       └── EmailDto.java
│
├── verification/
│   ├── EmailVerification.java
│   └── EmailVerificationRepository.java
│
└── passwordreset/
    ├── PasswordResetToken.java
    └── PasswordResetTokenRepository.java