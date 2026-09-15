// ClassMate Admin Portal - 2-Step Verification & Sponsor Management
document.addEventListener('DOMContentLoaded', () => {
  // Initialize Lucide icons
  lucide.createIcons();

  // Storage Keys
  const STORAGE_SUPABASE_URL = 'classmate_supabase_url';
  const STORAGE_SUPABASE_KEY = 'classmate_supabase_key';
  const STORAGE_LOCAL_SPONSOR = 'classmate_local_sponsor';
  const STORAGE_ADMIN_EMAIL = 'classmate_admin_email';
  const STORAGE_ADMIN_PASS = 'classmate_admin_password';
  const STORAGE_RECOVERY_KEY = 'classmate_recovery_key';
  const STORAGE_AUTH_SESSION = 'classmate_auth_session';
  const STORAGE_EMAILJS_SERVICE = 'classmate_emailjs_service_id';
  const STORAGE_EMAILJS_TEMPLATE = 'classmate_emailjs_template_id';
  const STORAGE_EMAILJS_PUBLIC_KEY = 'classmate_emailjs_public_key';

  // Default Credentials
  const DEFAULT_EMAIL = 'ssadman133@gmail.com';
  const DEFAULT_PASS = 'admin123';
  const DEFAULT_RECOVERY_KEY = 'CM-SAFE-2026-X891';

  // Auth DOM Elements
  const authOverlay = document.getElementById('authOverlay');
  const authStep1 = document.getElementById('authStep1');
  const authStep2 = document.getElementById('authStep2');
  const authStepRecovery = document.getElementById('authStepRecovery');

  const step1Form = document.getElementById('step1Form');
  const loginEmailInput = document.getElementById('loginEmailInput');
  const loginPasswordInput = document.getElementById('loginPasswordInput');
  const togglePasswordBtn = document.getElementById('togglePasswordBtn');
  const rememberMeCheck = document.getElementById('rememberMeCheck');

  const step2Form = document.getElementById('step2Form');
  const targetEmailLabel = document.getElementById('targetEmailLabel');
  const activeOtpDisplay = document.getElementById('activeOtpDisplay');
  const otpCodeInput = document.getElementById('otpCodeInput');
  const backToStep1Btn = document.getElementById('backToStep1Btn');
  const resendOtpBtn = document.getElementById('resendOtpBtn');

  const recoveryForm = document.getElementById('recoveryForm');
  const recoveryKeyInput = document.getElementById('recoveryKeyInput');
  const goToRecoveryBtn1 = document.getElementById('goToRecoveryBtn1');
  const goToRecoveryBtn2 = document.getElementById('goToRecoveryBtn2');
  const backToPasswordFromRecoveryBtn = document.getElementById('backToPasswordFromRecoveryBtn');

  const logoutBtn = document.getElementById('logoutBtn');
  const headerUserEmail = document.getElementById('headerUserEmail');

  // Sponsor Form & DOM
  const sponsorForm = document.getElementById('sponsorForm');
  const nameInput = document.getElementById('sponsorNameInput');
  const taglineInput = document.getElementById('sponsorTaglineInput');
  const websiteInput = document.getElementById('sponsorWebsiteInput');
  const imageInput = document.getElementById('sponsorImageInput');
  const imageFileInput = document.getElementById('sponsorImageFileInput');
  const startDateInput = document.getElementById('sponsorStartDateInput');
  const endDateInput = document.getElementById('sponsorEndDateInput');
  const activeSwitch = document.getElementById('sponsorActiveSwitch');
  const resetStatsBtn = document.getElementById('resetStatsBtn');
  const deleteSponsorBtn = document.getElementById('deleteSponsorBtn');
  const saveSponsorBtn = document.getElementById('saveSponsorBtn');
  const addNewSponsorBtn = document.getElementById('addNewSponsorBtn');
  const sponsorSlotsContainer = document.getElementById('sponsorSlotsContainer');

  // Preview Elements
  const previewBanner = document.getElementById('phonePreviewBanner');
  const previewFullBleedCard = document.getElementById('previewFullBleedCard');
  const previewFullBleedImg = document.getElementById('previewFullBleedImg');
  const previewDefaultCard = document.getElementById('previewDefaultCard');
  const previewTitle = document.getElementById('previewTitle');
  const previewSubtitle = document.getElementById('previewSubtitle');
  const previewHeroImg = document.getElementById('previewHeroImg');
  const previewInlineDots = document.getElementById('previewInlineDots');

  // Individual Ad Stats Elements
  const currentAdStatsTitle = document.getElementById('currentAdStatsTitle');
  const currentAdViews = document.getElementById('currentAdViews');
  const currentAdClicks = document.getElementById('currentAdClicks');
  const currentAdCtr = document.getElementById('currentAdCtr');

  // Stats Elements
  const totalViewsDisplay = document.getElementById('totalViewsDisplay');
  const totalClicksDisplay = document.getElementById('totalClicksDisplay');
  const ctrRateDisplay = document.getElementById('ctrRateDisplay');
  const statusIndicatorDot = document.getElementById('statusIndicatorDot');
  const statusBannerText = document.getElementById('statusBannerText');
  const statusBannerSubtext = document.getElementById('statusBannerSubtext');
  const connectionBadge = document.getElementById('connectionStatusBadge');
  const connectionText = document.getElementById('connectionStatusText');

  // Settings Modal Elements
  const settingsModal = document.getElementById('settingsModal');
  const openSettingsBtn = document.getElementById('openSettingsBtn');
  const closeSettingsBtn = document.getElementById('closeSettingsBtn');
  const saveSettingsBtn = document.getElementById('saveSettingsBtn');
  const settingsEmailInput = document.getElementById('settingsEmailInput');
  const settingsPasswordInput = document.getElementById('settingsPasswordInput');
  const displayMasterRecoveryKey = document.getElementById('displayMasterRecoveryKey');
  const copyRecoveryKeyBtn = document.getElementById('copyRecoveryKeyBtn');
  const regenerateKeyBtn = document.getElementById('regenerateKeyBtn');
  const supabaseUrlInput = document.getElementById('supabaseUrlInput');
  const supabaseAnonKeyInput = document.getElementById('supabaseAnonKeyInput');
  const copySqlBtn = document.getElementById('copySqlBtn');

  // EmailJS Settings Elements
  const emailjsServiceIdInput = document.getElementById('emailjsServiceIdInput');
  const emailjsTemplateIdInput = document.getElementById('emailjsTemplateIdInput');
  const emailjsPublicKeyInput = document.getElementById('emailjsPublicKeyInput');
  const otpBannerSubtext = document.getElementById('otpBannerSubtext');
  const otpBannerHint = document.getElementById('otpBannerHint');

  // Multi-Sponsor State Variables
  const STORAGE_LOCAL_SPONSORS_LIST = 'classmate_local_sponsors_list';

  let supabaseClient = null;
  let activeGeneratedOtp = null;
  let otpExpiryTime = null;

  let sponsorsList = [];
  let currentSponsorIndex = 0;
  let previewCarouselTimer = null;
  let previewActiveIndex = 0;

  // --- INITIALIZATION ---
  function init() {
    // Check existing auth session
    const isSessionValid = checkAuthentication();

    // Populate Credentials in Settings
    const registeredEmail = localStorage.getItem(STORAGE_ADMIN_EMAIL) || DEFAULT_EMAIL;
    const masterRecoveryKey = localStorage.getItem(STORAGE_RECOVERY_KEY) || DEFAULT_RECOVERY_KEY;

    loginEmailInput.value = registeredEmail;
    settingsEmailInput.value = registeredEmail;
    displayMasterRecoveryKey.innerText = masterRecoveryKey;
    headerUserEmail.innerText = registeredEmail;

    // Supabase Credentials
    const savedUrl = localStorage.getItem(STORAGE_SUPABASE_URL) || 'https://mgfvivzwakdbigtuuzai.supabase.co';
    const savedKey = localStorage.getItem(STORAGE_SUPABASE_KEY) || 'sb_publishable_TTqMNEtjm0MlvtdWfq3q2g_53UCvoCC';
    supabaseUrlInput.value = savedUrl;
    supabaseAnonKeyInput.value = savedKey;

    // EmailJS Credentials
    if (emailjsServiceIdInput) emailjsServiceIdInput.value = localStorage.getItem(STORAGE_EMAILJS_SERVICE) || '';
    if (emailjsTemplateIdInput) emailjsTemplateIdInput.value = localStorage.getItem(STORAGE_EMAILJS_TEMPLATE) || '';
    if (emailjsPublicKeyInput) emailjsPublicKeyInput.value = localStorage.getItem(STORAGE_EMAILJS_PUBLIC_KEY) || '';

    if (savedUrl && savedKey && window.supabase) {
      try {
        supabaseClient = window.supabase.createClient(savedUrl, savedKey);
        setConnectionState(true, 'Cloud Connected');
        fetchCloudSponsors();
      } catch (err) {
        console.warn('Supabase initialization fallback to local mode', err);
        setConnectionState(false, 'Local Demo Mode');
        loadLocalSponsors();
      }
    } else {
      setConnectionState(false, 'Local Demo Mode');
      loadLocalSponsors();
    }

    setupAuthHandlers();
    setupDashboardHandlers();
  }

  // --- AUTHENTICATION FLOW ---
  function checkAuthentication() {
    const session = sessionStorage.getItem(STORAGE_AUTH_SESSION) || localStorage.getItem(STORAGE_AUTH_SESSION);
    if (session === 'authenticated') {
      authOverlay.classList.add('hidden');
      return true;
    } else {
      authOverlay.classList.remove('hidden');
      showAuthStep(1);
      return false;
    }
  }

  function showAuthStep(step) {
    authStep1.classList.add('hidden');
    authStep2.classList.add('hidden');
    authStepRecovery.classList.add('hidden');

    if (step === 1) {
      authStep1.classList.remove('hidden');
    } else if (step === 2) {
      authStep2.classList.remove('hidden');
    } else if (step === 'recovery') {
      authStepRecovery.classList.remove('hidden');
    }
    lucide.createIcons();
  }

  function generateNewOtp() {
    // Generate secure 6-digit code
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    activeGeneratedOtp = code;
    otpExpiryTime = Date.now() + (5 * 60 * 1000); // 5 minutes valid

    const targetEmail = targetEmailLabel.innerText || DEFAULT_EMAIL;
    const serviceId = localStorage.getItem(STORAGE_EMAILJS_SERVICE);
    const templateId = localStorage.getItem(STORAGE_EMAILJS_TEMPLATE);
    const publicKey = localStorage.getItem(STORAGE_EMAILJS_PUBLIC_KEY);

    if (serviceId && templateId && publicKey && window.emailjs) {
      // Direct Email Delivery Mode via EmailJS
      activeOtpDisplay.innerText = '••••••';
      if (otpBannerSubtext) otpBannerSubtext.innerText = 'Security Code Dispatched:';
      if (otpBannerHint) otpBannerHint.innerText = `Confidential OTP sent directly to ${targetEmail}`;

      window.emailjs.init(publicKey);
      window.emailjs.send(serviceId, templateId, {
        to_email: targetEmail,
        email: targetEmail,
        user_email: targetEmail,
        to_name: 'Sadman Sakib',
        name: 'Sadman Sakib',
        otp_code: code,
        message: `Your ClassMate Admin verification code is: ${code}`,
        app_name: 'ClassMate Admin',
        time_limit: '5 minutes'
      }).then(() => {
        showToast(`Verification code emailed to ${targetEmail}!`, 'success');
      }).catch(err => {
        console.error('EmailJS sending failed:', err);
        const errDetails = err?.text || err?.message || 'Check EmailJS connection';
        // Fallback display if email service throws error
        activeOtpDisplay.innerText = code;
        if (otpBannerHint) otpBannerHint.innerText = `Email sending failed (${errDetails}). Use temporary display code above.`;
        showToast(`Email error: ${errDetails}`, 'warning');
      });
    } else {
      // Local/Testing Mode (keys not provided yet)
      activeOtpDisplay.innerText = code;
      if (otpBannerSubtext) otpBannerSubtext.innerText = 'Test Mode OTP (EmailJS not configured):';
      if (otpBannerHint) otpBannerHint.innerText = 'Click code to auto-fill or enter Security & API settings to send to real Gmail.';
      showToast(`6-Digit OTP: ${code} (Test Mode)`, 'info');
    }
  }

  function grantAccess(rememberSession = false) {
    if (rememberSession) {
      localStorage.setItem(STORAGE_AUTH_SESSION, 'authenticated');
    } else {
      sessionStorage.setItem(STORAGE_AUTH_SESSION, 'authenticated');
    }

    authOverlay.classList.add('opacity-0');
    setTimeout(() => {
      authOverlay.classList.add('hidden');
      authOverlay.classList.remove('opacity-0');
    }, 300);

    const email = localStorage.getItem(STORAGE_ADMIN_EMAIL) || DEFAULT_EMAIL;
    headerUserEmail.innerText = email;
    showToast('2-Step Verification Passed! Welcome, Admin.', 'success');
  }

  function setupAuthHandlers() {
    // Password toggle
    togglePasswordBtn.addEventListener('click', () => {
      const type = loginPasswordInput.type === 'password' ? 'text' : 'password';
      loginPasswordInput.type = type;
    });

    // Step 1 Form: Email & Password
    step1Form.addEventListener('submit', (e) => {
      e.preventDefault();

      const enteredEmail = loginEmailInput.value.trim().toLowerCase();
      const enteredPass = loginPasswordInput.value;

      const registeredEmail = (localStorage.getItem(STORAGE_ADMIN_EMAIL) || DEFAULT_EMAIL).toLowerCase();
      const registeredPass = localStorage.getItem(STORAGE_ADMIN_PASS) || DEFAULT_PASS;

      if (enteredEmail !== registeredEmail) {
        showToast('Unrecognized admin email address.', 'warning');
        return;
      }

      if (enteredPass !== registeredPass) {
        showToast('Incorrect password! Please try again.', 'warning');
        return;
      }

      // Step 1 Success -> Proceed to Step 2 OTP
      targetEmailLabel.innerText = enteredEmail;
      generateNewOtp();
      showAuthStep(2);
      otpCodeInput.value = '';
      otpCodeInput.focus();
    });

    // Click active OTP banner to auto-fill for convenience
    activeOtpDisplay.addEventListener('click', () => {
      if (activeGeneratedOtp) {
        otpCodeInput.value = activeGeneratedOtp;
      }
    });

    // Step 2 Form: Gmail OTP
    step2Form.addEventListener('submit', (e) => {
      e.preventDefault();

      const enteredOtp = otpCodeInput.value.trim();

      if (!activeGeneratedOtp || Date.now() > otpExpiryTime) {
        showToast('OTP code has expired. Please click Resend OTP.', 'warning');
        return;
      }

      if (enteredOtp === activeGeneratedOtp) {
        grantAccess(rememberMeCheck.checked);
      } else {
        showToast('Invalid 6-digit OTP code. Please check and retry.', 'warning');
      }
    });

    // Resend OTP
    resendOtpBtn.addEventListener('click', () => {
      generateNewOtp();
      otpCodeInput.value = '';
      otpCodeInput.focus();
    });

    // Back to Step 1
    backToStep1Btn.addEventListener('click', () => {
      showAuthStep(1);
    });

    // Recovery Screen Navigation
    goToRecoveryBtn1.addEventListener('click', () => {
      showAuthStep('recovery');
      recoveryKeyInput.value = '';
      recoveryKeyInput.focus();
    });

    goToRecoveryBtn2.addEventListener('click', () => {
      showAuthStep('recovery');
      recoveryKeyInput.value = '';
      recoveryKeyInput.focus();
    });

    backToPasswordFromRecoveryBtn.addEventListener('click', () => {
      showAuthStep(1);
    });

    // Recovery Form Submit
    recoveryForm.addEventListener('submit', (e) => {
      e.preventDefault();

      const enteredKey = recoveryKeyInput.value.trim().toUpperCase();
      const validKey = (localStorage.getItem(STORAGE_RECOVERY_KEY) || DEFAULT_RECOVERY_KEY).toUpperCase();

      if (enteredKey === validKey) {
        grantAccess(false);
        showToast('Emergency Master Key Accepted! Unlocked without OTP.', 'success');
      } else {
        showToast('Invalid Master Recovery Key! Please check your records.', 'warning');
      }
    });

    // Logout
    logoutBtn.addEventListener('click', () => {
      if (!confirm('Are you sure you want to log out of the Admin Portal?')) return;

      sessionStorage.removeItem(STORAGE_AUTH_SESSION);
      localStorage.removeItem(STORAGE_AUTH_SESSION);
      loginPasswordInput.value = '';
      otpCodeInput.value = '';
      showAuthStep(1);
      authOverlay.classList.remove('hidden');
      showToast('Logged out securely.', 'info');
    });
  }

  // --- DASHBOARD & SPONSOR MANAGEMENT ---
  function setConnectionState(isConnected, label) {
    if (isConnected) {
      connectionBadge.className = "flex items-center space-x-2 px-3 py-1.5 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20";
      connectionText.innerText = label;
    } else {
      connectionBadge.className = "flex items-center space-x-2 px-3 py-1.5 rounded-full text-xs font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20";
      connectionText.innerText = label;
    }
  }

  function loadLocalSponsors() {
    const listRaw = localStorage.getItem(STORAGE_LOCAL_SPONSORS_LIST);
    if (listRaw) {
      try {
        const parsed = JSON.parse(listRaw);
        if (Array.isArray(parsed)) {
          sponsorsList = parsed;
        }
      } catch (e) {}
    } else {
      sponsorsList = [];
    }
    if (currentSponsorIndex >= sponsorsList.length) currentSponsorIndex = Math.max(0, sponsorsList.length - 1);
    renderSponsorSlots();
    populateUI();
    startPreviewCarousel();
  }

  async function fetchCloudSponsors() {
    if (!supabaseClient) {
      loadLocalSponsors();
      return;
    }

    try {
      const { data, error } = await supabaseClient
        .from('sponsors')
        .select('*')
        .order('updated_at', { ascending: false });

      if (error) throw error;

      if (data) {
        sponsorsList = data.map((item, idx) => ({
          id: item.id || `sponsor_${idx + 1}`,
          name: item.name || '',
          tagline: item.tagline || '',
          website_url: item.website_url || '',
          image_url: item.image_url || '',
          start_date: item.start_date || '',
          end_date: item.end_date || '',
          is_active: item.is_active !== false,
          views_count: Number(item.views_count ?? item.impressions ?? 0),
          clicks_count: Number(item.clicks_count ?? item.clicks ?? 0)
        }));
        localStorage.setItem(STORAGE_LOCAL_SPONSORS_LIST, JSON.stringify(sponsorsList));
      } else {
        sponsorsList = [];
        localStorage.setItem(STORAGE_LOCAL_SPONSORS_LIST, JSON.stringify([]));
      }

      if (currentSponsorIndex >= sponsorsList.length) currentSponsorIndex = Math.max(0, sponsorsList.length - 1);
      renderSponsorSlots();
      populateUI();
      startPreviewCarousel();
    } catch (err) {
      console.error('Error fetching sponsors from Supabase:', err);
      showToast('Cloud sync fallback to local cached list.', 'warning');
      loadLocalSponsors();
    }
  }

  function saveFormToCurrentSponsor() {
    if (!sponsorsList[currentSponsorIndex]) return;
    sponsorsList[currentSponsorIndex].name = nameInput.value.trim();
    sponsorsList[currentSponsorIndex].tagline = taglineInput.value.trim();
    sponsorsList[currentSponsorIndex].website_url = websiteInput.value.trim();
    sponsorsList[currentSponsorIndex].image_url = imageInput.value.trim();
    sponsorsList[currentSponsorIndex].start_date = startDateInput.value;
    sponsorsList[currentSponsorIndex].end_date = endDateInput.value;
    sponsorsList[currentSponsorIndex].is_active = activeSwitch.checked;
  }

  function renderSponsorSlots() {
    if (!sponsorSlotsContainer) return;
    sponsorSlotsContainer.innerHTML = '';

    sponsorsList.forEach((sponsor, idx) => {
      const isSelected = idx === currentSponsorIndex;
      const chip = document.createElement('button');
      chip.type = 'button';
      chip.className = `px-3 py-1.5 rounded-xl text-xs font-medium transition flex items-center space-x-2 ${
        isSelected
          ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-400 shadow-sm shadow-cyan-500/20'
          : 'bg-slate-800/80 text-slate-300 border border-slate-700/60 hover:bg-slate-800 hover:text-white'
      }`;

      const statusDot = sponsor.is_active ? 'bg-emerald-400' : 'bg-slate-500';
      const shortName = (sponsor.name || `Ad #${idx + 1}`).split('•')[0].trim();

      chip.innerHTML = `
        <span class="w-2 h-2 rounded-full ${statusDot}"></span>
        <span class="font-bold">Ad ${idx + 1}</span>
      `;

      chip.addEventListener('click', () => {
        saveFormToCurrentSponsor();
        currentSponsorIndex = idx;
        renderSponsorSlots();
        populateUI();
        updatePreview(sponsorsList[currentSponsorIndex]);
      });

      sponsorSlotsContainer.appendChild(chip);
    });

    // Delete button is always available whenever there is at least one sponsor
    if (sponsorsList.length > 0) {
      deleteSponsorBtn.classList.remove('hidden');
    } else {
      deleteSponsorBtn.classList.add('hidden');
    }
  }

  function populateUI() {
    if (sponsorsList.length === 0) {
      nameInput.value = '';
      taglineInput.value = '';
      websiteInput.value = '';
      imageInput.value = '';
      startDateInput.value = '';
      endDateInput.value = '';
      activeSwitch.checked = false;
      updateStatsAndPreview();
      return;
    }

    const current = sponsorsList[currentSponsorIndex] || sponsorsList[0];
    if (!current) return;

    nameInput.value = current.name || '';
    taglineInput.value = current.tagline || '';
    websiteInput.value = current.website_url || '';
    imageInput.value = current.image_url || '';
    startDateInput.value = current.start_date || '';
    endDateInput.value = current.end_date || '';
    activeSwitch.checked = current.is_active !== false;

    updateStatsAndPreview();
  }

  function updateStatsAndPreview() {
    saveFormToCurrentSponsor();

    // Aggregated stats across all sponsors
    let totalViews = 0;
    let totalClicks = 0;
    let activeCount = 0;

    sponsorsList.forEach(s => {
      totalViews += Number(s.views_count || 0);
      totalClicks += Number(s.clicks_count || 0);
      if (s.is_active) activeCount++;
    });

    const ctr = totalViews > 0 ? ((totalClicks / totalViews) * 100).toFixed(1) : '0.0';

    totalViewsDisplay.innerText = Number(totalViews).toLocaleString();
    totalClicksDisplay.innerText = Number(totalClicks).toLocaleString();
    ctrRateDisplay.innerText = `${ctr}%`;

    // Individual stats for the currently selected Ad
    const current = sponsorsList[currentSponsorIndex];
    if (current && currentAdViews && currentAdClicks && currentAdCtr) {
      const curViews = Number(current.views_count || 0);
      const curClicks = Number(current.clicks_count || 0);
      const curCtr = curViews > 0 ? ((curClicks / curViews) * 100).toFixed(1) : '0.0';
      const curName = (current.name || `Ad #${currentSponsorIndex + 1}`).trim();

      if (currentAdStatsTitle) {
        currentAdStatsTitle.innerText = `Performance: ${curName}`;
      }
      currentAdViews.innerText = curViews.toLocaleString();
      currentAdClicks.innerText = curClicks.toLocaleString();
      currentAdCtr.innerText = `${curCtr}%`;
    } else if (currentAdViews && currentAdClicks && currentAdCtr) {
      if (currentAdStatsTitle) currentAdStatsTitle.innerText = 'No Ad Selected';
      currentAdViews.innerText = '0';
      currentAdClicks.innerText = '0';
      currentAdCtr.innerText = '0.0%';
    }

    if (activeCount > 0) {
      statusIndicatorDot.className = "w-3 h-3 rounded-full bg-emerald-400 animate-pulse";
      if (activeCount > 1) {
        statusBannerText.innerText = `${activeCount} in Rotation`;
        statusBannerSubtext.innerText = "Auto-sliding every 4s in student apps";
      } else {
        statusBannerText.innerText = "1 Active";
        statusBannerSubtext.innerText = "Visible to all student devices";
      }
      previewBanner.classList.remove('opacity-20', 'grayscale');
    } else {
      statusIndicatorDot.className = "w-3 h-3 rounded-full bg-slate-500";
      statusBannerText.innerText = "All Disabled";
      statusBannerSubtext.innerText = "Carousel hidden on student devices";
      previewBanner.classList.add('opacity-20', 'grayscale');
    }

    if (current) {
      updatePreview(current);
    } else {
      updatePreview(null);
    }
  }

  function updatePreview(sponsor) {
    if (!sponsor || !sponsor.is_active) {
      const firstActive = sponsorsList.find(s => s.is_active);
      if (firstActive) {
        sponsor = firstActive;
      }
    }

    if (!sponsor) {
      if (previewDefaultCard) previewDefaultCard.classList.remove('hidden');
      if (previewFullBleedCard) previewFullBleedCard.classList.add('hidden');
      if (previewTitle) previewTitle.innerText = 'NO ACTIVE SPONSORS';
      if (previewSubtitle) previewSubtitle.innerText = 'Banner hidden on devices';
      return;
    }

    const hasCustomImage = sponsor.image_url && isValidUrl(sponsor.image_url);

    if (hasCustomImage) {
      // Show full-bleed poster preview (matches Android Compose full-bleed layout)
      if (previewFullBleedCard) previewFullBleedCard.classList.remove('hidden');
      if (previewDefaultCard) previewDefaultCard.classList.add('hidden');
      if (previewFullBleedImg) previewFullBleedImg.src = sponsor.image_url;
    } else {
      // Show clean default card
      if (previewFullBleedCard) previewFullBleedCard.classList.add('hidden');
      if (previewDefaultCard) previewDefaultCard.classList.remove('hidden');

      const rawName = sponsor.name || 'ClassMate Partner';
      if (previewTitle) previewTitle.innerText = rawName;
      if (previewSubtitle) previewSubtitle.innerText = 'Featured Sponsor';

      const defaultHeroImg = "https://images.unsplash.com/photo-1523240795612-9a054b0db644?q=80&w=800&auto=format&fit=crop";
      if (previewHeroImg) {
        previewHeroImg.src = defaultHeroImg;
      }
    }
  }

  function startPreviewCarousel() {
    if (previewCarouselTimer) {
      clearInterval(previewCarouselTimer);
      previewCarouselTimer = null;
    }

    function getActiveSponsors() {
      return sponsorsList.filter(s => s.is_active);
    }

    function step() {
      const activeList = getActiveSponsors();
      if (activeList.length === 0) {
        updatePreview(null);
        renderPreviewDots(0, 0);
        return;
      }
      previewActiveIndex = (previewActiveIndex + 1) % activeList.length;
      animateSlidePreview(activeList[previewActiveIndex]);
      renderPreviewDots(activeList.length, previewActiveIndex);
    }

    const activeList = getActiveSponsors();
    if (activeList.length > 0) {
      previewActiveIndex = 0;
      updatePreview(activeList[0]);
      renderPreviewDots(activeList.length, 0);
    } else {
      updatePreview(null);
      renderPreviewDots(0, 0);
    }

    if (activeList.length > 1) {
      // 4-second rotation matching Android app!
      previewCarouselTimer = setInterval(step, 4000);
    }
  }

  function animateSlidePreview(sponsor) {
    if (!previewBanner) return;
    previewBanner.classList.add('opacity-0', '-translate-x-2');
    setTimeout(() => {
      updatePreview(sponsor);
      previewBanner.classList.remove('opacity-0', '-translate-x-2');
    }, 250);
  }

  function renderPreviewDots(total, current) {
    if (!previewInlineDots) return;
    previewInlineDots.innerHTML = '';
    const dotsCount = Math.max(total, 6);
    for (let i = 0; i < dotsCount; i++) {
      const dot = document.createElement('div');
      const isSelected = total > 0 ? (i % total === current) : (i === 0);
      dot.className = `h-1.5 rounded-full transition-all duration-300 ${
        isSelected ? 'w-3.5 bg-cyan-400 shadow-sm shadow-cyan-400/50' : 'w-1.5 bg-slate-600/70'
      }`;
      previewInlineDots.appendChild(dot);
    }
  }

  function isValidUrl(string) {
    if (!string) return false;
    if (string.startsWith('data:image/')) return true;
    try {
      new URL(string);
      return true;
    } catch (_) {
      return false;
    }
  }

  // Compress & convert selected image file directly to optimized Base64 / Data URI
  async function handleImageFileUpload(file) {
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      showToast('Please select a valid image file (JPG, PNG, WebP).', 'warning');
      return;
    }

    showToast('Processing & uploading image to Supabase...', 'info');

    // 1. Process & compress image with canvas to crisp lightweight JPEG
    const compressPromise = new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const img = new Image();
        img.onload = () => {
          const maxW = 1200;
          const maxH = 480;
          let targetW = img.width;
          let targetH = img.height;

          if (targetW > maxW) {
            targetH = Math.round((targetH * maxW) / targetW);
            targetW = maxW;
          }
          if (targetH > maxH) {
            targetW = Math.round((targetW * maxH) / targetH);
            targetH = maxH;
          }

          const canvas = document.createElement('canvas');
          canvas.width = targetW;
          canvas.height = targetH;
          const ctx = canvas.getContext('2d');
          ctx.drawImage(img, 0, 0, targetW, targetH);

          canvas.toBlob((blob) => {
            const dataUrl = canvas.toDataURL('image/jpeg', 0.82);
            resolve({ blob, dataUrl });
          }, 'image/jpeg', 0.82);
        };
        img.src = e.target.result;
      };
      reader.readAsDataURL(file);
    });

    try {
      const { blob, dataUrl } = await compressPromise;
      let finalImageUrl = dataUrl;

      // 2. Direct upload to Supabase Storage Bucket ('sponsors')
      const targetUrl = supabaseUrlInput.value.trim() || 'https://mgfvivzwakdbigtuuzai.supabase.co';
      const targetKey = supabaseAnonKeyInput.value.trim() || 'sb_publishable_TTqMNEtjm0MlvtdWfq3q2g_53UCvoCC';
      const fileName = `banner_${Date.now()}_${Math.random().toString(36).substring(2, 8)}.jpg`;
      let uploadSuccess = false;

      // Method A: Direct fetch to Supabase Storage REST API (Reliable & fast)
      try {
        const uploadEndpoint = `${targetUrl}/storage/v1/object/sponsors/${fileName}`;
        const uploadRes = await fetch(uploadEndpoint, {
          method: 'POST',
          headers: {
            'apikey': targetKey,
            'Authorization': `Bearer ${targetKey}`,
            'Content-Type': 'image/jpeg',
            'x-upsert': 'true'
          },
          body: blob
        });

        if (uploadRes.ok) {
          finalImageUrl = `${targetUrl}/storage/v1/object/public/sponsors/${fileName}`;
          uploadSuccess = true;
          console.log('Direct REST upload succeeded:', finalImageUrl);
        } else {
          const errText = await uploadRes.text();
          console.warn('Direct REST upload response:', uploadRes.status, errText);
        }
      } catch (restErr) {
        console.warn('Direct REST upload error:', restErr);
      }

      // Method B: Fallback via Supabase JS SDK storage client
      if (!uploadSuccess && supabaseClient && supabaseClient.storage) {
        try {
          const { data: uploadData, error: uploadError } = await supabaseClient
            .storage
            .from('sponsors')
            .upload(fileName, blob, {
              contentType: 'image/jpeg',
              cacheControl: '31536000',
              upsert: true
            });

          if (!uploadError && uploadData) {
            const { data: publicUrlData } = supabaseClient.storage.from('sponsors').getPublicUrl(fileName);
            if (publicUrlData && publicUrlData.publicUrl) {
              finalImageUrl = publicUrlData.publicUrl;
              uploadSuccess = true;
            }
          }
        } catch (sdkErr) {
          console.warn('SDK upload error:', sdkErr);
        }
      }

      if (uploadSuccess) {
        console.log('Successfully uploaded image to Supabase Storage:', finalImageUrl);
      } else {
        console.warn('Supabase storage upload failed, falling back to local compressed image');
      }

      // 3. Set URL into input and update sponsor object
      if (sponsorsList.length === 0) {
        sponsorsList.push({
          id: 'sponsor_1',
          name: 'Featured Sponsor',
          tagline: '',
          website_url: websiteInput.value.trim() || 'https://example.com',
          image_url: finalImageUrl,
          start_date: new Date().toISOString().split('T')[0],
          end_date: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
          is_active: true,
          views_count: 0,
          clicks_count: 0
        });
        currentSponsorIndex = 0;
        renderSponsorSlots();
        populateUI();
      } else {
        imageInput.value = finalImageUrl;
        updateStatsAndPreview();
      }

      if (finalImageUrl.startsWith('http')) {
        showToast('Clean Supabase image URL generated! Click "Publish All to App".', 'success');
      } else {
        showToast('Image ready! Note: Create a public "sponsors" bucket in Supabase for short URLs.', 'success');
      }
    } catch (err) {
      console.error('Error handling image upload:', err);
      showToast('Error processing image. Please try again.', 'error');
    }
  }

  function setupDashboardHandlers() {
    nameInput.addEventListener('input', () => {
      updateStatsAndPreview();
      renderSponsorSlots();
    });
    taglineInput.addEventListener('input', updateStatsAndPreview);
    imageInput.addEventListener('input', updateStatsAndPreview);

    if (imageFileInput) {
      imageFileInput.addEventListener('change', (e) => {
        if (e.target.files && e.target.files[0]) {
          handleImageFileUpload(e.target.files[0]);
        }
      });
    }
    activeSwitch.addEventListener('change', () => {
      updateStatsAndPreview();
      renderSponsorSlots();
      startPreviewCarousel();
    });

    // Add New Sponsor Slot
    // Add New Ad Campaign
    addNewSponsorBtn.addEventListener('click', () => {
      saveFormToCurrentSponsor();

      const newId = `sponsor_${Date.now()}`;
      const newSponsor = {
        id: newId,
        name: `Ad #${sponsorsList.length + 1}`,
        tagline: '',
        website_url: 'https://',
        image_url: '',
        start_date: new Date().toISOString().split('T')[0],
        end_date: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
        is_active: true,
        views_count: 0,
        clicks_count: 0
      };

      sponsorsList.push(newSponsor);
      currentSponsorIndex = sponsorsList.length - 1;
      localStorage.setItem(STORAGE_LOCAL_SPONSORS_LIST, JSON.stringify(sponsorsList));

      renderSponsorSlots();
      populateUI();
      startPreviewCarousel();
      showToast('New ad campaign created and added to rotation!', 'success');
    });

    // Delete Sponsor / Ad Campaign
    deleteSponsorBtn.addEventListener('click', async () => {
      if (sponsorsList.length === 0) {
        showToast('No active ads to delete.', 'info');
        return;
      }

      const deletingSponsor = sponsorsList[currentSponsorIndex];
      const adTitle = deletingSponsor.name || `Ad #${currentSponsorIndex + 1}`;
      if (!confirm(`Are you sure you want to permanently delete "${adTitle}"?`)) return;

      const deletingId = deletingSponsor.id;
      sponsorsList.splice(currentSponsorIndex, 1);
      if (currentSponsorIndex >= sponsorsList.length) {
        currentSponsorIndex = Math.max(0, sponsorsList.length - 1);
      }

      localStorage.setItem(STORAGE_LOCAL_SPONSORS_LIST, JSON.stringify(sponsorsList));

      if (supabaseClient) {
        try {
          await supabaseClient.from('sponsors').delete().eq('id', deletingId);
        } catch (err) {
          console.warn('Cloud deletion warning:', err);
        }
      }

      renderSponsorSlots();
      populateUI();
      startPreviewCarousel();
      showToast('Ad campaign deleted successfully!', 'info');
    });

    // Save All Sponsors & Publish
    sponsorForm.addEventListener('submit', async (e) => {
      e.preventDefault();

      saveFormToCurrentSponsor();

      saveSponsorBtn.disabled = true;
      saveSponsorBtn.innerHTML = `
        <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        <span>Publishing...</span>
      `;

      localStorage.setItem(STORAGE_LOCAL_SPONSORS_LIST, JSON.stringify(sponsorsList));
      localStorage.setItem(STORAGE_LOCAL_SPONSOR, JSON.stringify(sponsorsList[0]));

      if (supabaseClient) {
        try {
          // Upsert all active sponsors to Supabase
          const records = sponsorsList.map(s => ({
            id: s.id,
            name: s.name,
            tagline: s.tagline || '',
            image_url: s.image_url || '',
            website_url: s.website_url,
            start_date: s.start_date,
            end_date: s.end_date,
            is_active: s.is_active,
            updated_at: new Date().toISOString()
          }));

          const { error } = await supabaseClient
            .from('sponsors')
            .upsert(records, { onConflict: 'id' });

          if (error) throw error;
          showToast(`Published ${sponsorsList.length} sponsors! Auto-scrolls every 4s in app.`, 'success');
        } catch (err) {
          console.error(err);
          showToast('Saved locally. Cloud sync failed: ' + err.message, 'warning');
        }
      } else {
        showToast('Saved locally! Connect Supabase to push live to student phones.', 'success');
      }

      saveSponsorBtn.disabled = false;
      saveSponsorBtn.innerHTML = `
        <i data-lucide="cloud-upload" class="w-4 h-4 mr-2"></i>
        <span>Publish All to App</span>
      `;
      lucide.createIcons();
      renderSponsorSlots();
      startPreviewCarousel();
    });

    // Reset Views/Clicks Stats
    resetStatsBtn.addEventListener('click', async () => {
      if (!confirm('Are you sure you want to reset Views and Clicks counters for this sponsor?')) return;

      const current = sponsorsList[currentSponsorIndex];
      if (current) {
        current.views_count = 0;
        current.clicks_count = 0;
      }

      localStorage.setItem(STORAGE_LOCAL_SPONSORS_LIST, JSON.stringify(sponsorsList));

      if (supabaseClient && current) {
        try {
          await supabaseClient
            .from('sponsors')
            .update({ views_count: 0, clicks_count: 0, impressions: 0, clicks: 0 })
            .eq('id', current.id);
        } catch (e) {}
      }

      updateStatsAndPreview();
      showToast('Analytics counters reset to 0.', 'info');
    });

    // Settings Modal
    openSettingsBtn.addEventListener('click', () => {
      settingsModal.classList.remove('hidden');
      settingsModal.classList.add('flex');
    });

    closeSettingsBtn.addEventListener('click', () => {
      settingsModal.classList.add('hidden');
      settingsModal.classList.remove('flex');
    });

    // Regenerate Master Recovery Key
    regenerateKeyBtn.addEventListener('click', () => {
      if (!confirm('Regenerate a new Master Recovery Key? Remember to save the new key!')) return;

      const randomPart = Math.random().toString(36).substring(2, 6).toUpperCase();
      const numPart = Math.floor(1000 + Math.random() * 9000);
      const newKey = `CM-SAFE-${numPart}-${randomPart}`;

      localStorage.setItem(STORAGE_RECOVERY_KEY, newKey);
      displayMasterRecoveryKey.innerText = newKey;
      showToast('New Master Recovery Key generated! Please write it down.', 'warning');
    });

    // Copy Recovery Key
    copyRecoveryKeyBtn.addEventListener('click', () => {
      const key = displayMasterRecoveryKey.innerText;
      navigator.clipboard.writeText(key).then(() => {
        document.getElementById('copyRecoveryKeyText').innerText = 'Copied!';
        setTimeout(() => {
          document.getElementById('copyRecoveryKeyText').innerText = 'Copy Key';
        }, 2000);
      });
    });

    // Save Settings (Credentials + Supabase)
    saveSettingsBtn.addEventListener('click', () => {
      const newEmail = settingsEmailInput.value.trim();
      const newPass = settingsPasswordInput.value.trim();

      if (newEmail) {
        localStorage.setItem(STORAGE_ADMIN_EMAIL, newEmail);
        loginEmailInput.value = newEmail;
        headerUserEmail.innerText = newEmail;
      }

      if (newPass) {
        localStorage.setItem(STORAGE_ADMIN_PASS, newPass);
      }

      const url = supabaseUrlInput.value.trim();
      const key = supabaseAnonKeyInput.value.trim();
      localStorage.setItem(STORAGE_SUPABASE_URL, url);
      localStorage.setItem(STORAGE_SUPABASE_KEY, key);

      // Save EmailJS Settings
      if (emailjsServiceIdInput) localStorage.setItem(STORAGE_EMAILJS_SERVICE, emailjsServiceIdInput.value.trim());
      if (emailjsTemplateIdInput) localStorage.setItem(STORAGE_EMAILJS_TEMPLATE, emailjsTemplateIdInput.value.trim());
      if (emailjsPublicKeyInput) localStorage.setItem(STORAGE_EMAILJS_PUBLIC_KEY, emailjsPublicKeyInput.value.trim());

      settingsModal.classList.add('hidden');
      settingsModal.classList.remove('flex');

      init();
      showToast('All security credentials and database settings saved!', 'success');
    });

    // Copy SQL Script
    copySqlBtn.addEventListener('click', () => {
      const sqlCode = `create table if not exists sponsors (
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

alter table sponsors enable row level security;
drop policy if exists "Allow public read" on sponsors;
drop policy if exists "Allow admin write" on sponsors;
create policy "Allow public read" on sponsors for select using (true);
create policy "Allow admin write" on sponsors for all using (true);

-- Create public storage bucket for short clean image links
insert into storage.buckets (id, name, public) 
values ('sponsors', 'sponsors', true)
on conflict (id) do nothing;

drop policy if exists "Public Access" on storage.objects;
create policy "Public Access" on storage.objects for select using (bucket_id = 'sponsors');

drop policy if exists "Public Upload" on storage.objects;
create policy "Public Upload" on storage.objects for insert with check (bucket_id = 'sponsors');`;

      navigator.clipboard.writeText(sqlCode).then(() => {
        document.getElementById('copySqlText').innerText = 'Copied!';
        setTimeout(() => {
          document.getElementById('copySqlText').innerText = 'Copy SQL';
        }, 2000);
      });
    });
  }

  // Toast Helper
  function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    const toastMessage = document.getElementById('toastMessage');
    const toastIcon = document.getElementById('toastIcon');

    toastMessage.innerText = message;

    if (type === 'success') {
      toastIcon.setAttribute('data-lucide', 'check-circle');
      toastIcon.className = 'w-5 h-5 text-emerald-400';
    } else if (type === 'warning') {
      toastIcon.setAttribute('data-lucide', 'alert-triangle');
      toastIcon.className = 'w-5 h-5 text-amber-400';
    } else {
      toastIcon.setAttribute('data-lucide', 'info');
      toastIcon.className = 'w-5 h-5 text-cyan-400';
    }

    lucide.createIcons();

    toast.classList.remove('translate-y-20', 'opacity-0');
    toast.classList.add('translate-y-0', 'opacity-100');

    setTimeout(() => {
      toast.classList.remove('translate-y-0', 'opacity-100');
      toast.classList.add('translate-y-20', 'opacity-0');
    }, 3500);
  }

  // Run
  init();
});
