# ClassMate • Sponsor & Partner Web Admin Portal

একটি আধুনিক, হালকা ও সম্পূর্ণ কার্যকরী ওয়েব অ্যাডমিন পোর্টাল—যার মাধ্যমে আপনি যেকোনো ব্রাউজার থেকে স্পনসরশিপ ব্যানার, প্রোমো অফার এবং ছাত্রছাত্রীদের এনগেজমেন্ট অ্যানালিটিক্স (Views ও Clicks) নিয়ন্ত্রণ করতে পারবেন।

---

## 🚀 Vercel-এ হোস্ট করার সহজ নিয়ম (Step-by-Step):

1. **গিটহাবে রিপোজিটরি পুশ করুন:**
   - AI Studio-র উপরে ডানদিকের সেটিংস/মেনু থেকে **"Push to GitHub"** এ ক্লিক করুন।
   - সম্পূর্ণ প্রজেক্ট সহ এই `admin-web` ফোল্ডারটিও আপনার GitHub রিপোজিটরিতে চলে যাবে।

2. **Vercel-এ প্রজেক্ট ইমপোর্ট করুন:**
   - [Vercel.com](https://vercel.com)-এ যান এবং আপনার GitHub দিয়ে লগইন করুন।
   - **"Add New Project"** এ ক্লিক করে আপনার রিপোজিটরি সিলেক্ট করুন।

3. **Root Directory সিলেক্ট করুন (খুব গুরুত্বপূর্ণ):**
   - Vercel কনফিগারেশন পেজে **"Root Directory"** অপশনের পাশে `Edit` বাটনে ক্লিক করুন।
   - ড্রপডাউন বা টেক্সটবক্সে **`admin-web`** সিলেক্ট করে দিন।
   - **"Deploy"** বাটনে ক্লিক করুন।
   - মাত্র ২০ সেকেন্ডে আপনার অ্যাডমিন ওয়েবসাইট লাইভ হয়ে যাবে এবং একটি ফ্রি ডোমেইন লিঙ্ক (`https://your-name.vercel.app`) পেয়ে যাবেন!

---

## 🗄️ Supabase ডাটাবেস সংযোগ (১ মিনিটের সেটআপ):

1. [Supabase.com](https://supabase.com)-এ একটি ফ্রি অ্যাকাউন্ট খুলে নতুন প্রজেক্ট তৈরি করুন।
2. বাম পাশের মেনু থেকে **SQL Editor**-এ যান এবং **New Query** চাপুন।
3. নিচের SQL কোডটুকু পেস্ট করে **Run** চাপুন:

```sql
create table if not exists sponsors (
  id text primary key,
  name text not null,
  image_url text default '',
  website_url text not null,
  start_date text default '',
  end_date text default '',
  is_active boolean default true,
  views_count bigint default 0,
  clicks_count bigint default 0,
  updated_at timestamp with time zone default now()
);

-- ছাত্রছাত্রীদের অ্যাপের জন্য পাবলিক রিড পারমিশন চালু করুন
alter table sponsors enable row level security;
create policy "Allow public read" on sponsors for select using (true);
create policy "Allow admin write" on sponsors for all using (true);
```

4. এরপর Supabase-এর **Project Settings -> API** থেকে `Project URL` এবং `anon public key` কপি করে আপনার অ্যাডমিন পোর্টালের **"Database API"** বক্সে পেস্ট করে সেভ করে দিন।

---

## 📱 ফিচারসমূহ:
* **লাইভ মোবাইল প্রিভিউ:** নতুন কোনো স্পনসর যুক্ত করার সময় ছাত্রছাত্রীদের ফোনে ব্যানারটি কেমন দেখাবে তা সাথে সাথে স্ক্রিনে দেখা যায়।
* **ভিউ ও ক্লিক অ্যানালিটিক্স:** কতজন ছাত্র ব্যানারটি দেখেছে (Views) এবং কতজন ওয়েবসাইট ভিজিট করেছে (Clicks)—তার লাইভ কাউন্টার।
* **এক ক্লিকে বন্ধ/চালু:** যে কোনো সময় স্পনসর অফ করে দিলে ছাত্রছাত্রীদের ফোন থেকে সাথে সাথে ব্যানার গায়েব হয়ে যাবে।
