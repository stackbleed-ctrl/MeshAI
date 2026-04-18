<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1400 500" width="1400" height="500">
<defs>

  <radialGradient id="bg" cx="50%" cy="50%" r="80%">
    <stop offset="0%" stop-color="#0b1d35"/>
    <stop offset="60%" stop-color="#06101e"/>
    <stop offset="100%" stop-color="#030710"/>
  </radialGradient>
  <radialGradient id="bgAtm" cx="50%" cy="50%" r="35%">
    <stop offset="0%" stop-color="#0a3566" stop-opacity="0.18"/>
    <stop offset="100%" stop-color="#000" stop-opacity="0"/>
  </radialGradient>
  <radialGradient id="aGrad" cx="40%" cy="30%" r="65%">
    <stop offset="0%" stop-color="#b8ffe0"/>
    <stop offset="40%" stop-color="#3ddc84"/>
    <stop offset="100%" stop-color="#1a6640"/>
  </radialGradient>
  <radialGradient id="rGrad" cx="50%" cy="40%" r="60%">
    <stop offset="0%" stop-color="#88f0ff"/>
    <stop offset="100%" stop-color="#0088bb"/>
  </radialGradient>
  <linearGradient id="borderGrad" x1="0%" y1="0%" x2="100%" y2="0%">
    <stop offset="0%" stop-color="#3ddc84" stop-opacity="0"/>
    <stop offset="20%" stop-color="#3ddc84" stop-opacity="0.5"/>
    <stop offset="50%" stop-color="#00d4ff" stop-opacity="0.8"/>
    <stop offset="80%" stop-color="#3ddc84" stop-opacity="0.5"/>
    <stop offset="100%" stop-color="#3ddc84" stop-opacity="0"/>
  </linearGradient>
  <linearGradient id="borderGradB" x1="0%" y1="0%" x2="100%" y2="0%">
    <stop offset="0%" stop-color="#3ddc84" stop-opacity="0"/>
    <stop offset="30%" stop-color="#00d4ff" stop-opacity="0.4"/>
    <stop offset="70%" stop-color="#3ddc84" stop-opacity="0.4"/>
    <stop offset="100%" stop-color="#3ddc84" stop-opacity="0"/>
  </linearGradient>


  <filter id="ag" x="-90%" y="-90%" width="280%" height="280%">
    <feGaussianBlur stdDeviation="15" result="b1"/>
    <feGaussianBlur stdDeviation="6" in="SourceGraphic" result="b2"/>
    <feColorMatrix in="b1" type="matrix" values="0 0 0 0 0.12  0 0 0 0 0.86  0 0 0 0 0.47  0 0 0 1.4 0" result="cb"/>
    <feMerge><feMergeNode in="cb"/><feMergeNode in="cb"/><feMergeNode in="b2"/><feMergeNode in="SourceGraphic"/></feMerge>
  </filter>
  <filter id="cg" x="-70%" y="-70%" width="240%" height="240%">
    <feGaussianBlur stdDeviation="7" result="b"/>
    <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
  </filter>
  <filter id="lg" x="-5%" y="-300%" width="110%" height="700%">
    <feGaussianBlur stdDeviation="2.5" result="b"/>
    <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
  </filter>
  <filter id="tg" x="-8%" y="-40%" width="116%" height="180%">
    <feGaussianBlur stdDeviation="18" result="b1"/>
    <feGaussianBlur stdDeviation="8" in="SourceGraphic" result="b2"/>
    <feColorMatrix in="b1" type="matrix" values="0 0 0 0 0  0 0 0 0 0.8  0 0 0 0 1  0 0 0 2.5 0" result="cb"/>
    <feMerge><feMergeNode in="cb"/><feMergeNode in="cb"/><feMergeNode in="b2"/><feMergeNode in="SourceGraphic"/></feMerge>
  </filter>
  <filter id="stg" x="-15%" y="-80%" width="130%" height="260%">
    <feGaussianBlur stdDeviation="6" result="b"/>
    <feColorMatrix in="b" type="matrix" values="0 0 0 0 0  0 0 0 0 0.7  0 0 0 0 1  0 0 0 1.5 0" result="cb"/>
    <feMerge><feMergeNode in="cb"/><feMergeNode in="SourceGraphic"/></feMerge>
  </filter>
  <filter id="pg" x="-300%" y="-300%" width="700%" height="700%">
    <feGaussianBlur stdDeviation="4"/>
  </filter>


  <symbol id="ph" viewBox="-15 -26 30 52" overflow="visible">
    <rect x="-13" y="-24" width="26" height="48" rx="5" fill="none" stroke="currentColor" stroke-width="2"/>
    <rect x="-10" y="-20" width="20" height="34" rx="1.5" fill="currentColor" opacity="0.08"/>
    <rect x="-6" y="-22" width="12" height="3" rx="1.5" fill="currentColor" opacity="0.45"/>
    <circle cx="5.5" cy="-22.5" r="2" fill="currentColor" opacity="0.35"/>
    <line x1="-7" y1="20" x2="7" y2="20" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" opacity="0.5"/>
    <!-- neural net on screen -->
    <circle cx="0" cy="-12" r="1.5" fill="currentColor" opacity="0.6"/>
    <circle cx="-5" cy="-7" r="1.3" fill="currentColor" opacity="0.55"/>
    <circle cx="5" cy="-7" r="1.3" fill="currentColor" opacity="0.55"/>
    <circle cx="-5" cy="-2" r="1.3" fill="currentColor" opacity="0.5"/>
    <circle cx="5" cy="-2" r="1.3" fill="currentColor" opacity="0.5"/>
    <circle cx="0" cy="3" r="1.5" fill="currentColor" opacity="0.45"/>
    <line x1="0" y1="-12" x2="-5" y2="-7" stroke="currentColor" stroke-width="0.7" opacity="0.45"/>
    <line x1="0" y1="-12" x2="5" y2="-7" stroke="currentColor" stroke-width="0.7" opacity="0.45"/>
    <line x1="-5" y1="-7" x2="-5" y2="-2" stroke="currentColor" stroke-width="0.7" opacity="0.4"/>
    <line x1="5" y1="-7" x2="5" y2="-2" stroke="currentColor" stroke-width="0.7" opacity="0.4"/>
    <line x1="-5" y1="-7" x2="5" y2="-7" stroke="currentColor" stroke-width="0.7" opacity="0.35"/>
    <line x1="-5" y1="-2" x2="5" y2="-2" stroke="currentColor" stroke-width="0.7" opacity="0.35"/>
    <line x1="-5" y1="-2" x2="0" y2="3" stroke="currentColor" stroke-width="0.7" opacity="0.35"/>
    <line x1="5" y1="-2" x2="0" y2="3" stroke="currentColor" stroke-width="0.7" opacity="0.35"/>
    <line x1="-5" y1="-7" x2="0" y2="-2" stroke="currentColor" stroke-width="0.4" opacity="0.25"/>
    <line x1="5" y1="-7" x2="0" y2="-2" stroke="currentColor" stroke-width="0.4" opacity="0.25"/>
  </symbol>

  <path id="fp1" d="M100,170 C180,140 230,110 280,90" fill="none"/>
  <path id="fp2" d="M385,175 C455,215 495,240 530,265" fill="none"/>
  <path id="fp3" d="M530,265 C650,256 760,248 870,240" fill="none"/>
  <path id="fp4" d="M870,240 C940,215 975,195 1015,175" fill="none"/>
  <path id="fp5" d="M1015,175 C1055,138 1080,114 1110,90" fill="none"/>
  <path id="fp6" d="M280,90 C330,128 358,152 385,175" fill="none"/>
  <path id="fp7" d="M1215,295 C1252,258 1275,232 1300,200" fill="none"/>
  <path id="fp8" d="M695,95 C770,155 815,195 870,240" fill="none"/>
</defs>
<rect width="1400" height="500" fill="url(#bg)"/>
<rect width="1400" height="500" fill="url(#bgAtm)"/>
<!-- hex grid -->
<polygon points="-15.0,-26.0 -30.0,0.0 -60.0,0.0 -75.0,-26.0 -60.0,-52.0 -30.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="-15.0,26.0 -30.0,52.0 -60.0,52.0 -75.0,26.0 -60.0,0.0 -30.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="-15.0,77.9 -30.0,103.9 -60.0,103.9 -75.0,77.9 -60.0,52.0 -30.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="-15.0,129.9 -30.0,155.9 -60.0,155.9 -75.0,129.9 -60.0,103.9 -30.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="-15.0,181.9 -30.0,207.8 -60.0,207.8 -75.0,181.9 -60.0,155.9 -30.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="-15.0,233.8 -30.0,259.8 -60.0,259.8 -75.0,233.8 -60.0,207.8 -30.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="-15.0,285.8 -30.0,311.8 -60.0,311.8 -75.0,285.8 -60.0,259.8 -30.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="-15.0,337.7 -30.0,363.7 -60.0,363.7 -75.0,337.7 -60.0,311.8 -30.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="-15.0,389.7 -30.0,415.7 -60.0,415.7 -75.0,389.7 -60.0,363.7 -30.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="-15.0,441.7 -30.0,467.7 -60.0,467.7 -75.0,441.7 -60.0,415.7 -30.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="-15.0,493.6 -30.0,519.6 -60.0,519.6 -75.0,493.6 -60.0,467.7 -30.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="-15.0,545.6 -30.0,571.6 -60.0,571.6 -75.0,545.6 -60.0,519.6 -30.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="-15.0,597.6 -30.0,623.5 -60.0,623.5 -75.0,597.6 -60.0,571.6 -30.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="30.0,-52.0 15.0,-26.0 -15.0,-26.0 -30.0,-52.0 -15.0,-77.9 15.0,-77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="30.0,0.0 15.0,26.0 -15.0,26.0 -30.0,0.0 -15.0,-26.0 15.0,-26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="30.0,52.0 15.0,77.9 -15.0,77.9 -30.0,52.0 -15.0,26.0 15.0,26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="30.0,103.9 15.0,129.9 -15.0,129.9 -30.0,103.9 -15.0,77.9 15.0,77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="30.0,155.9 15.0,181.9 -15.0,181.9 -30.0,155.9 -15.0,129.9 15.0,129.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="30.0,207.8 15.0,233.8 -15.0,233.8 -30.0,207.8 -15.0,181.9 15.0,181.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="30.0,259.8 15.0,285.8 -15.0,285.8 -30.0,259.8 -15.0,233.8 15.0,233.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="30.0,311.8 15.0,337.7 -15.0,337.7 -30.0,311.8 -15.0,285.8 15.0,285.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="30.0,363.7 15.0,389.7 -15.0,389.7 -30.0,363.7 -15.0,337.7 15.0,337.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="30.0,415.7 15.0,441.7 -15.0,441.7 -30.0,415.7 -15.0,389.7 15.0,389.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="30.0,467.7 15.0,493.6 -15.0,493.6 -30.0,467.7 -15.0,441.7 15.0,441.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="30.0,519.6 15.0,545.6 -15.0,545.6 -30.0,519.6 -15.0,493.6 15.0,493.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="30.0,571.6 15.0,597.6 -15.0,597.6 -30.0,571.6 -15.0,545.6 15.0,545.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="75.0,-26.0 60.0,0.0 30.0,0.0 15.0,-26.0 30.0,-52.0 60.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="75.0,26.0 60.0,52.0 30.0,52.0 15.0,26.0 30.0,0.0 60.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="75.0,77.9 60.0,103.9 30.0,103.9 15.0,77.9 30.0,52.0 60.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="75.0,129.9 60.0,155.9 30.0,155.9 15.0,129.9 30.0,103.9 60.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="75.0,181.9 60.0,207.8 30.0,207.8 15.0,181.9 30.0,155.9 60.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="75.0,233.8 60.0,259.8 30.0,259.8 15.0,233.8 30.0,207.8 60.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="75.0,285.8 60.0,311.8 30.0,311.8 15.0,285.8 30.0,259.8 60.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="75.0,337.7 60.0,363.7 30.0,363.7 15.0,337.7 30.0,311.8 60.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="75.0,389.7 60.0,415.7 30.0,415.7 15.0,389.7 30.0,363.7 60.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="75.0,441.7 60.0,467.7 30.0,467.7 15.0,441.7 30.0,415.7 60.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="75.0,493.6 60.0,519.6 30.0,519.6 15.0,493.6 30.0,467.7 60.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="75.0,545.6 60.0,571.6 30.0,571.6 15.0,545.6 30.0,519.6 60.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="75.0,597.6 60.0,623.5 30.0,623.5 15.0,597.6 30.0,571.6 60.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="120.0,-52.0 105.0,-26.0 75.0,-26.0 60.0,-52.0 75.0,-77.9 105.0,-77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="120.0,0.0 105.0,26.0 75.0,26.0 60.0,0.0 75.0,-26.0 105.0,-26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="120.0,52.0 105.0,77.9 75.0,77.9 60.0,52.0 75.0,26.0 105.0,26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="120.0,103.9 105.0,129.9 75.0,129.9 60.0,103.9 75.0,77.9 105.0,77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="120.0,155.9 105.0,181.9 75.0,181.9 60.0,155.9 75.0,129.9 105.0,129.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="120.0,207.8 105.0,233.8 75.0,233.8 60.0,207.8 75.0,181.9 105.0,181.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="120.0,259.8 105.0,285.8 75.0,285.8 60.0,259.8 75.0,233.8 105.0,233.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="120.0,311.8 105.0,337.7 75.0,337.7 60.0,311.8 75.0,285.8 105.0,285.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="120.0,363.7 105.0,389.7 75.0,389.7 60.0,363.7 75.0,337.7 105.0,337.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="120.0,415.7 105.0,441.7 75.0,441.7 60.0,415.7 75.0,389.7 105.0,389.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="120.0,467.7 105.0,493.6 75.0,493.6 60.0,467.7 75.0,441.7 105.0,441.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="120.0,519.6 105.0,545.6 75.0,545.6 60.0,519.6 75.0,493.6 105.0,493.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="120.0,571.6 105.0,597.6 75.0,597.6 60.0,571.6 75.0,545.6 105.0,545.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="165.0,-26.0 150.0,0.0 120.0,0.0 105.0,-26.0 120.0,-52.0 150.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="165.0,26.0 150.0,52.0 120.0,52.0 105.0,26.0 120.0,0.0 150.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="165.0,77.9 150.0,103.9 120.0,103.9 105.0,77.9 120.0,52.0 150.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="165.0,129.9 150.0,155.9 120.0,155.9 105.0,129.9 120.0,103.9 150.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="165.0,181.9 150.0,207.8 120.0,207.8 105.0,181.9 120.0,155.9 150.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="165.0,233.8 150.0,259.8 120.0,259.8 105.0,233.8 120.0,207.8 150.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="165.0,285.8 150.0,311.8 120.0,311.8 105.0,285.8 120.0,259.8 150.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="165.0,337.7 150.0,363.7 120.0,363.7 105.0,337.7 120.0,311.8 150.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="165.0,389.7 150.0,415.7 120.0,415.7 105.0,389.7 120.0,363.7 150.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="165.0,441.7 150.0,467.7 120.0,467.7 105.0,441.7 120.0,415.7 150.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="165.0,493.6 150.0,519.6 120.0,519.6 105.0,493.6 120.0,467.7 150.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="165.0,545.6 150.0,571.6 120.0,571.6 105.0,545.6 120.0,519.6 150.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="165.0,597.6 150.0,623.5 120.0,623.5 105.0,597.6 120.0,571.6 150.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="210.0,-52.0 195.0,-26.0 165.0,-26.0 150.0,-52.0 165.0,-77.9 195.0,-77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="210.0,0.0 195.0,26.0 165.0,26.0 150.0,0.0 165.0,-26.0 195.0,-26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="210.0,52.0 195.0,77.9 165.0,77.9 150.0,52.0 165.0,26.0 195.0,26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="210.0,103.9 195.0,129.9 165.0,129.9 150.0,103.9 165.0,77.9 195.0,77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="210.0,155.9 195.0,181.9 165.0,181.9 150.0,155.9 165.0,129.9 195.0,129.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="210.0,207.8 195.0,233.8 165.0,233.8 150.0,207.8 165.0,181.9 195.0,181.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="210.0,259.8 195.0,285.8 165.0,285.8 150.0,259.8 165.0,233.8 195.0,233.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="210.0,311.8 195.0,337.7 165.0,337.7 150.0,311.8 165.0,285.8 195.0,285.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="210.0,363.7 195.0,389.7 165.0,389.7 150.0,363.7 165.0,337.7 195.0,337.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="210.0,415.7 195.0,441.7 165.0,441.7 150.0,415.7 165.0,389.7 195.0,389.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="210.0,467.7 195.0,493.6 165.0,493.6 150.0,467.7 165.0,441.7 195.0,441.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="210.0,519.6 195.0,545.6 165.0,545.6 150.0,519.6 165.0,493.6 195.0,493.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="210.0,571.6 195.0,597.6 165.0,597.6 150.0,571.6 165.0,545.6 195.0,545.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="255.0,-26.0 240.0,0.0 210.0,0.0 195.0,-26.0 210.0,-52.0 240.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="255.0,26.0 240.0,52.0 210.0,52.0 195.0,26.0 210.0,0.0 240.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="255.0,77.9 240.0,103.9 210.0,103.9 195.0,77.9 210.0,52.0 240.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="255.0,129.9 240.0,155.9 210.0,155.9 195.0,129.9 210.0,103.9 240.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="255.0,181.9 240.0,207.8 210.0,207.8 195.0,181.9 210.0,155.9 240.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="255.0,233.8 240.0,259.8 210.0,259.8 195.0,233.8 210.0,207.8 240.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="255.0,285.8 240.0,311.8 210.0,311.8 195.0,285.8 210.0,259.8 240.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="255.0,337.7 240.0,363.7 210.0,363.7 195.0,337.7 210.0,311.8 240.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="255.0,389.7 240.0,415.7 210.0,415.7 195.0,389.7 210.0,363.7 240.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="255.0,441.7 240.0,467.7 210.0,467.7 195.0,441.7 210.0,415.7 240.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="255.0,493.6 240.0,519.6 210.0,519.6 195.0,493.6 210.0,467.7 240.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="255.0,545.6 240.0,571.6 210.0,571.6 195.0,545.6 210.0,519.6 240.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="255.0,597.6 240.0,623.5 210.0,623.5 195.0,597.6 210.0,571.6 240.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="300.0,-52.0 285.0,-26.0 255.0,-26.0 240.0,-52.0 255.0,-77.9 285.0,-77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="300.0,0.0 285.0,26.0 255.0,26.0 240.0,0.0 255.0,-26.0 285.0,-26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="300.0,52.0 285.0,77.9 255.0,77.9 240.0,52.0 255.0,26.0 285.0,26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="300.0,103.9 285.0,129.9 255.0,129.9 240.0,103.9 255.0,77.9 285.0,77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="300.0,155.9 285.0,181.9 255.0,181.9 240.0,155.9 255.0,129.9 285.0,129.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="300.0,207.8 285.0,233.8 255.0,233.8 240.0,207.8 255.0,181.9 285.0,181.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="300.0,259.8 285.0,285.8 255.0,285.8 240.0,259.8 255.0,233.8 285.0,233.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="300.0,311.8 285.0,337.7 255.0,337.7 240.0,311.8 255.0,285.8 285.0,285.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="300.0,363.7 285.0,389.7 255.0,389.7 240.0,363.7 255.0,337.7 285.0,337.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="300.0,415.7 285.0,441.7 255.0,441.7 240.0,415.7 255.0,389.7 285.0,389.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="300.0,467.7 285.0,493.6 255.0,493.6 240.0,467.7 255.0,441.7 285.0,441.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="300.0,519.6 285.0,545.6 255.0,545.6 240.0,519.6 255.0,493.6 285.0,493.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="300.0,571.6 285.0,597.6 255.0,597.6 240.0,571.6 255.0,545.6 285.0,545.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="345.0,-26.0 330.0,0.0 300.0,0.0 285.0,-26.0 300.0,-52.0 330.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="345.0,26.0 330.0,52.0 300.0,52.0 285.0,26.0 300.0,0.0 330.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="345.0,77.9 330.0,103.9 300.0,103.9 285.0,77.9 300.0,52.0 330.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="345.0,129.9 330.0,155.9 300.0,155.9 285.0,129.9 300.0,103.9 330.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="345.0,181.9 330.0,207.8 300.0,207.8 285.0,181.9 300.0,155.9 330.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="345.0,233.8 330.0,259.8 300.0,259.8 285.0,233.8 300.0,207.8 330.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="345.0,285.8 330.0,311.8 300.0,311.8 285.0,285.8 300.0,259.8 330.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="345.0,337.7 330.0,363.7 300.0,363.7 285.0,337.7 300.0,311.8 330.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="345.0,389.7 330.0,415.7 300.0,415.7 285.0,389.7 300.0,363.7 330.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="345.0,441.7 330.0,467.7 300.0,467.7 285.0,441.7 300.0,415.7 330.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="345.0,493.6 330.0,519.6 300.0,519.6 285.0,493.6 300.0,467.7 330.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="345.0,545.6 330.0,571.6 300.0,571.6 285.0,545.6 300.0,519.6 330.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="345.0,597.6 330.0,623.5 300.0,623.5 285.0,597.6 300.0,571.6 330.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="390.0,-52.0 375.0,-26.0 345.0,-26.0 330.0,-52.0 345.0,-77.9 375.0,-77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="390.0,0.0 375.0,26.0 345.0,26.0 330.0,0.0 345.0,-26.0 375.0,-26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="390.0,52.0 375.0,77.9 345.0,77.9 330.0,52.0 345.0,26.0 375.0,26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="390.0,103.9 375.0,129.9 345.0,129.9 330.0,103.9 345.0,77.9 375.0,77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="390.0,155.9 375.0,181.9 345.0,181.9 330.0,155.9 345.0,129.9 375.0,129.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="390.0,207.8 375.0,233.8 345.0,233.8 330.0,207.8 345.0,181.9 375.0,181.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="390.0,259.8 375.0,285.8 345.0,285.8 330.0,259.8 345.0,233.8 375.0,233.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="390.0,311.8 375.0,337.7 345.0,337.7 330.0,311.8 345.0,285.8 375.0,285.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="390.0,363.7 375.0,389.7 345.0,389.7 330.0,363.7 345.0,337.7 375.0,337.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="390.0,415.7 375.0,441.7 345.0,441.7 330.0,415.7 345.0,389.7 375.0,389.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="390.0,467.7 375.0,493.6 345.0,493.6 330.0,467.7 345.0,441.7 375.0,441.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="390.0,519.6 375.0,545.6 345.0,545.6 330.0,519.6 345.0,493.6 375.0,493.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="390.0,571.6 375.0,597.6 345.0,597.6 330.0,571.6 345.0,545.6 375.0,545.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="435.0,-26.0 420.0,0.0 390.0,0.0 375.0,-26.0 390.0,-52.0 420.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="435.0,26.0 420.0,52.0 390.0,52.0 375.0,26.0 390.0,0.0 420.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="435.0,77.9 420.0,103.9 390.0,103.9 375.0,77.9 390.0,52.0 420.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="435.0,129.9 420.0,155.9 390.0,155.9 375.0,129.9 390.0,103.9 420.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="435.0,181.9 420.0,207.8 390.0,207.8 375.0,181.9 390.0,155.9 420.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="435.0,233.8 420.0,259.8 390.0,259.8 375.0,233.8 390.0,207.8 420.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="435.0,285.8 420.0,311.8 390.0,311.8 375.0,285.8 390.0,259.8 420.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="435.0,337.7 420.0,363.7 390.0,363.7 375.0,337.7 390.0,311.8 420.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="435.0,389.7 420.0,415.7 390.0,415.7 375.0,389.7 390.0,363.7 420.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="435.0,441.7 420.0,467.7 390.0,467.7 375.0,441.7 390.0,415.7 420.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="435.0,493.6 420.0,519.6 390.0,519.6 375.0,493.6 390.0,467.7 420.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="435.0,545.6 420.0,571.6 390.0,571.6 375.0,545.6 390.0,519.6 420.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="435.0,597.6 420.0,623.5 390.0,623.5 375.0,597.6 390.0,571.6 420.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="480.0,-52.0 465.0,-26.0 435.0,-26.0 420.0,-52.0 435.0,-77.9 465.0,-77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="480.0,0.0 465.0,26.0 435.0,26.0 420.0,0.0 435.0,-26.0 465.0,-26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="480.0,52.0 465.0,77.9 435.0,77.9 420.0,52.0 435.0,26.0 465.0,26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="480.0,103.9 465.0,129.9 435.0,129.9 420.0,103.9 435.0,77.9 465.0,77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="480.0,155.9 465.0,181.9 435.0,181.9 420.0,155.9 435.0,129.9 465.0,129.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="480.0,207.8 465.0,233.8 435.0,233.8 420.0,207.8 435.0,181.9 465.0,181.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="480.0,259.8 465.0,285.8 435.0,285.8 420.0,259.8 435.0,233.8 465.0,233.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="480.0,311.8 465.0,337.7 435.0,337.7 420.0,311.8 435.0,285.8 465.0,285.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="480.0,363.7 465.0,389.7 435.0,389.7 420.0,363.7 435.0,337.7 465.0,337.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="480.0,415.7 465.0,441.7 435.0,441.7 420.0,415.7 435.0,389.7 465.0,389.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="480.0,467.7 465.0,493.6 435.0,493.6 420.0,467.7 435.0,441.7 465.0,441.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="480.0,519.6 465.0,545.6 435.0,545.6 420.0,519.6 435.0,493.6 465.0,493.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="480.0,571.6 465.0,597.6 435.0,597.6 420.0,571.6 435.0,545.6 465.0,545.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="525.0,-26.0 510.0,0.0 480.0,0.0 465.0,-26.0 480.0,-52.0 510.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="525.0,26.0 510.0,52.0 480.0,52.0 465.0,26.0 480.0,0.0 510.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="525.0,77.9 510.0,103.9 480.0,103.9 465.0,77.9 480.0,52.0 510.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="525.0,129.9 510.0,155.9 480.0,155.9 465.0,129.9 480.0,103.9 510.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="525.0,181.9 510.0,207.8 480.0,207.8 465.0,181.9 480.0,155.9 510.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="525.0,233.8 510.0,259.8 480.0,259.8 465.0,233.8 480.0,207.8 510.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="525.0,285.8 510.0,311.8 480.0,311.8 465.0,285.8 480.0,259.8 510.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="525.0,337.7 510.0,363.7 480.0,363.7 465.0,337.7 480.0,311.8 510.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="525.0,389.7 510.0,415.7 480.0,415.7 465.0,389.7 480.0,363.7 510.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="525.0,441.7 510.0,467.7 480.0,467.7 465.0,441.7 480.0,415.7 510.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="525.0,493.6 510.0,519.6 480.0,519.6 465.0,493.6 480.0,467.7 510.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="525.0,545.6 510.0,571.6 480.0,571.6 465.0,545.6 480.0,519.6 510.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="525.0,597.6 510.0,623.5 480.0,623.5 465.0,597.6 480.0,571.6 510.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="570.0,-52.0 555.0,-26.0 525.0,-26.0 510.0,-52.0 525.0,-77.9 555.0,-77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="570.0,0.0 555.0,26.0 525.0,26.0 510.0,0.0 525.0,-26.0 555.0,-26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="570.0,52.0 555.0,77.9 525.0,77.9 510.0,52.0 525.0,26.0 555.0,26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="570.0,103.9 555.0,129.9 525.0,129.9 510.0,103.9 525.0,77.9 555.0,77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="570.0,155.9 555.0,181.9 525.0,181.9 510.0,155.9 525.0,129.9 555.0,129.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="570.0,207.8 555.0,233.8 525.0,233.8 510.0,207.8 525.0,181.9 555.0,181.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="570.0,259.8 555.0,285.8 525.0,285.8 510.0,259.8 525.0,233.8 555.0,233.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="570.0,311.8 555.0,337.7 525.0,337.7 510.0,311.8 525.0,285.8 555.0,285.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="570.0,363.7 555.0,389.7 525.0,389.7 510.0,363.7 525.0,337.7 555.0,337.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="570.0,415.7 555.0,441.7 525.0,441.7 510.0,415.7 525.0,389.7 555.0,389.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="570.0,467.7 555.0,493.6 525.0,493.6 510.0,467.7 525.0,441.7 555.0,441.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="570.0,519.6 555.0,545.6 525.0,545.6 510.0,519.6 525.0,493.6 555.0,493.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="570.0,571.6 555.0,597.6 525.0,597.6 510.0,571.6 525.0,545.6 555.0,545.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="615.0,-26.0 600.0,0.0 570.0,0.0 555.0,-26.0 570.0,-52.0 600.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="615.0,26.0 600.0,52.0 570.0,52.0 555.0,26.0 570.0,0.0 600.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="615.0,77.9 600.0,103.9 570.0,103.9 555.0,77.9 570.0,52.0 600.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="615.0,129.9 600.0,155.9 570.0,155.9 555.0,129.9 570.0,103.9 600.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="615.0,181.9 600.0,207.8 570.0,207.8 555.0,181.9 570.0,155.9 600.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="615.0,233.8 600.0,259.8 570.0,259.8 555.0,233.8 570.0,207.8 600.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="615.0,285.8 600.0,311.8 570.0,311.8 555.0,285.8 570.0,259.8 600.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="615.0,337.7 600.0,363.7 570.0,363.7 555.0,337.7 570.0,311.8 600.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="615.0,389.7 600.0,415.7 570.0,415.7 555.0,389.7 570.0,363.7 600.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="615.0,441.7 600.0,467.7 570.0,467.7 555.0,441.7 570.0,415.7 600.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="615.0,493.6 600.0,519.6 570.0,519.6 555.0,493.6 570.0,467.7 600.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="615.0,545.6 600.0,571.6 570.0,571.6 555.0,545.6 570.0,519.6 600.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="615.0,597.6 600.0,623.5 570.0,623.5 555.0,597.6 570.0,571.6 600.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="660.0,-52.0 645.0,-26.0 615.0,-26.0 600.0,-52.0 615.0,-77.9 645.0,-77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="660.0,0.0 645.0,26.0 615.0,26.0 600.0,0.0 615.0,-26.0 645.0,-26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="660.0,52.0 645.0,77.9 615.0,77.9 600.0,52.0 615.0,26.0 645.0,26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="660.0,103.9 645.0,129.9 615.0,129.9 600.0,103.9 615.0,77.9 645.0,77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="660.0,155.9 645.0,181.9 615.0,181.9 600.0,155.9 615.0,129.9 645.0,129.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="660.0,207.8 645.0,233.8 615.0,233.8 600.0,207.8 615.0,181.9 645.0,181.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="660.0,259.8 645.0,285.8 615.0,285.8 600.0,259.8 615.0,233.8 645.0,233.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="660.0,311.8 645.0,337.7 615.0,337.7 600.0,311.8 615.0,285.8 645.0,285.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="660.0,363.7 645.0,389.7 615.0,389.7 600.0,363.7 615.0,337.7 645.0,337.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="660.0,415.7 645.0,441.7 615.0,441.7 600.0,415.7 615.0,389.7 645.0,389.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="660.0,467.7 645.0,493.6 615.0,493.6 600.0,467.7 615.0,441.7 645.0,441.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="660.0,519.6 645.0,545.6 615.0,545.6 600.0,519.6 615.0,493.6 645.0,493.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="660.0,571.6 645.0,597.6 615.0,597.6 600.0,571.6 615.0,545.6 645.0,545.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="705.0,-26.0 690.0,0.0 660.0,0.0 645.0,-26.0 660.0,-52.0 690.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="705.0,26.0 690.0,52.0 660.0,52.0 645.0,26.0 660.0,0.0 690.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="705.0,77.9 690.0,103.9 660.0,103.9 645.0,77.9 660.0,52.0 690.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="705.0,129.9 690.0,155.9 660.0,155.9 645.0,129.9 660.0,103.9 690.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="705.0,181.9 690.0,207.8 660.0,207.8 645.0,181.9 660.0,155.9 690.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="705.0,233.8 690.0,259.8 660.0,259.8 645.0,233.8 660.0,207.8 690.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="705.0,285.8 690.0,311.8 660.0,311.8 645.0,285.8 660.0,259.8 690.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="705.0,337.7 690.0,363.7 660.0,363.7 645.0,337.7 660.0,311.8 690.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="705.0,389.7 690.0,415.7 660.0,415.7 645.0,389.7 660.0,363.7 690.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="705.0,441.7 690.0,467.7 660.0,467.7 645.0,441.7 660.0,415.7 690.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="705.0,493.6 690.0,519.6 660.0,519.6 645.0,493.6 660.0,467.7 690.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="705.0,545.6 690.0,571.6 660.0,571.6 645.0,545.6 660.0,519.6 690.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="705.0,597.6 690.0,623.5 660.0,623.5 645.0,597.6 660.0,571.6 690.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="750.0,-52.0 735.0,-26.0 705.0,-26.0 690.0,-52.0 705.0,-77.9 735.0,-77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="750.0,0.0 735.0,26.0 705.0,26.0 690.0,0.0 705.0,-26.0 735.0,-26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="750.0,52.0 735.0,77.9 705.0,77.9 690.0,52.0 705.0,26.0 735.0,26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="750.0,103.9 735.0,129.9 705.0,129.9 690.0,103.9 705.0,77.9 735.0,77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="750.0,155.9 735.0,181.9 705.0,181.9 690.0,155.9 705.0,129.9 735.0,129.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="750.0,207.8 735.0,233.8 705.0,233.8 690.0,207.8 705.0,181.9 735.0,181.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="750.0,259.8 735.0,285.8 705.0,285.8 690.0,259.8 705.0,233.8 735.0,233.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="750.0,311.8 735.0,337.7 705.0,337.7 690.0,311.8 705.0,285.8 735.0,285.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="750.0,363.7 735.0,389.7 705.0,389.7 690.0,363.7 705.0,337.7 735.0,337.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="750.0,415.7 735.0,441.7 705.0,441.7 690.0,415.7 705.0,389.7 735.0,389.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="750.0,467.7 735.0,493.6 705.0,493.6 690.0,467.7 705.0,441.7 735.0,441.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="750.0,519.6 735.0,545.6 705.0,545.6 690.0,519.6 705.0,493.6 735.0,493.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="750.0,571.6 735.0,597.6 705.0,597.6 690.0,571.6 705.0,545.6 735.0,545.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="795.0,-26.0 780.0,0.0 750.0,0.0 735.0,-26.0 750.0,-52.0 780.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="795.0,26.0 780.0,52.0 750.0,52.0 735.0,26.0 750.0,0.0 780.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="795.0,77.9 780.0,103.9 750.0,103.9 735.0,77.9 750.0,52.0 780.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="795.0,129.9 780.0,155.9 750.0,155.9 735.0,129.9 750.0,103.9 780.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="795.0,181.9 780.0,207.8 750.0,207.8 735.0,181.9 750.0,155.9 780.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="795.0,233.8 780.0,259.8 750.0,259.8 735.0,233.8 750.0,207.8 780.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="795.0,285.8 780.0,311.8 750.0,311.8 735.0,285.8 750.0,259.8 780.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="795.0,337.7 780.0,363.7 750.0,363.7 735.0,337.7 750.0,311.8 780.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="795.0,389.7 780.0,415.7 750.0,415.7 735.0,389.7 750.0,363.7 780.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="795.0,441.7 780.0,467.7 750.0,467.7 735.0,441.7 750.0,415.7 780.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="795.0,493.6 780.0,519.6 750.0,519.6 735.0,493.6 750.0,467.7 780.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="795.0,545.6 780.0,571.6 750.0,571.6 735.0,545.6 750.0,519.6 780.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="795.0,597.6 780.0,623.5 750.0,623.5 735.0,597.6 750.0,571.6 780.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="840.0,-52.0 825.0,-26.0 795.0,-26.0 780.0,-52.0 795.0,-77.9 825.0,-77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="840.0,0.0 825.0,26.0 795.0,26.0 780.0,0.0 795.0,-26.0 825.0,-26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="840.0,52.0 825.0,77.9 795.0,77.9 780.0,52.0 795.0,26.0 825.0,26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="840.0,103.9 825.0,129.9 795.0,129.9 780.0,103.9 795.0,77.9 825.0,77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="840.0,155.9 825.0,181.9 795.0,181.9 780.0,155.9 795.0,129.9 825.0,129.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="840.0,207.8 825.0,233.8 795.0,233.8 780.0,207.8 795.0,181.9 825.0,181.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="840.0,259.8 825.0,285.8 795.0,285.8 780.0,259.8 795.0,233.8 825.0,233.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="840.0,311.8 825.0,337.7 795.0,337.7 780.0,311.8 795.0,285.8 825.0,285.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="840.0,363.7 825.0,389.7 795.0,389.7 780.0,363.7 795.0,337.7 825.0,337.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="840.0,415.7 825.0,441.7 795.0,441.7 780.0,415.7 795.0,389.7 825.0,389.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="840.0,467.7 825.0,493.6 795.0,493.6 780.0,467.7 795.0,441.7 825.0,441.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="840.0,519.6 825.0,545.6 795.0,545.6 780.0,519.6 795.0,493.6 825.0,493.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="840.0,571.6 825.0,597.6 795.0,597.6 780.0,571.6 795.0,545.6 825.0,545.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="885.0,-26.0 870.0,0.0 840.0,0.0 825.0,-26.0 840.0,-52.0 870.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="885.0,26.0 870.0,52.0 840.0,52.0 825.0,26.0 840.0,0.0 870.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="885.0,77.9 870.0,103.9 840.0,103.9 825.0,77.9 840.0,52.0 870.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="885.0,129.9 870.0,155.9 840.0,155.9 825.0,129.9 840.0,103.9 870.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="885.0,181.9 870.0,207.8 840.0,207.8 825.0,181.9 840.0,155.9 870.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="885.0,233.8 870.0,259.8 840.0,259.8 825.0,233.8 840.0,207.8 870.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="885.0,285.8 870.0,311.8 840.0,311.8 825.0,285.8 840.0,259.8 870.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="885.0,337.7 870.0,363.7 840.0,363.7 825.0,337.7 840.0,311.8 870.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="885.0,389.7 870.0,415.7 840.0,415.7 825.0,389.7 840.0,363.7 870.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="885.0,441.7 870.0,467.7 840.0,467.7 825.0,441.7 840.0,415.7 870.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="885.0,493.6 870.0,519.6 840.0,519.6 825.0,493.6 840.0,467.7 870.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="885.0,545.6 870.0,571.6 840.0,571.6 825.0,545.6 840.0,519.6 870.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="885.0,597.6 870.0,623.5 840.0,623.5 825.0,597.6 840.0,571.6 870.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="930.0,-52.0 915.0,-26.0 885.0,-26.0 870.0,-52.0 885.0,-77.9 915.0,-77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="930.0,0.0 915.0,26.0 885.0,26.0 870.0,0.0 885.0,-26.0 915.0,-26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="930.0,52.0 915.0,77.9 885.0,77.9 870.0,52.0 885.0,26.0 915.0,26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="930.0,103.9 915.0,129.9 885.0,129.9 870.0,103.9 885.0,77.9 915.0,77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="930.0,155.9 915.0,181.9 885.0,181.9 870.0,155.9 885.0,129.9 915.0,129.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="930.0,207.8 915.0,233.8 885.0,233.8 870.0,207.8 885.0,181.9 915.0,181.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="930.0,259.8 915.0,285.8 885.0,285.8 870.0,259.8 885.0,233.8 915.0,233.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="930.0,311.8 915.0,337.7 885.0,337.7 870.0,311.8 885.0,285.8 915.0,285.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="930.0,363.7 915.0,389.7 885.0,389.7 870.0,363.7 885.0,337.7 915.0,337.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="930.0,415.7 915.0,441.7 885.0,441.7 870.0,415.7 885.0,389.7 915.0,389.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="930.0,467.7 915.0,493.6 885.0,493.6 870.0,467.7 885.0,441.7 915.0,441.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="930.0,519.6 915.0,545.6 885.0,545.6 870.0,519.6 885.0,493.6 915.0,493.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="930.0,571.6 915.0,597.6 885.0,597.6 870.0,571.6 885.0,545.6 915.0,545.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="975.0,-26.0 960.0,0.0 930.0,0.0 915.0,-26.0 930.0,-52.0 960.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="975.0,26.0 960.0,52.0 930.0,52.0 915.0,26.0 930.0,0.0 960.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="975.0,77.9 960.0,103.9 930.0,103.9 915.0,77.9 930.0,52.0 960.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="975.0,129.9 960.0,155.9 930.0,155.9 915.0,129.9 930.0,103.9 960.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="975.0,181.9 960.0,207.8 930.0,207.8 915.0,181.9 930.0,155.9 960.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="975.0,233.8 960.0,259.8 930.0,259.8 915.0,233.8 930.0,207.8 960.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="975.0,285.8 960.0,311.8 930.0,311.8 915.0,285.8 930.0,259.8 960.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="975.0,337.7 960.0,363.7 930.0,363.7 915.0,337.7 930.0,311.8 960.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="975.0,389.7 960.0,415.7 930.0,415.7 915.0,389.7 930.0,363.7 960.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="975.0,441.7 960.0,467.7 930.0,467.7 915.0,441.7 930.0,415.7 960.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="975.0,493.6 960.0,519.6 930.0,519.6 915.0,493.6 930.0,467.7 960.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="975.0,545.6 960.0,571.6 930.0,571.6 915.0,545.6 930.0,519.6 960.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="975.0,597.6 960.0,623.5 930.0,623.5 915.0,597.6 930.0,571.6 960.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1020.0,-52.0 1005.0,-26.0 975.0,-26.0 960.0,-52.0 975.0,-77.9 1005.0,-77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1020.0,0.0 1005.0,26.0 975.0,26.0 960.0,0.0 975.0,-26.0 1005.0,-26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1020.0,52.0 1005.0,77.9 975.0,77.9 960.0,52.0 975.0,26.0 1005.0,26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1020.0,103.9 1005.0,129.9 975.0,129.9 960.0,103.9 975.0,77.9 1005.0,77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1020.0,155.9 1005.0,181.9 975.0,181.9 960.0,155.9 975.0,129.9 1005.0,129.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1020.0,207.8 1005.0,233.8 975.0,233.8 960.0,207.8 975.0,181.9 1005.0,181.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1020.0,259.8 1005.0,285.8 975.0,285.8 960.0,259.8 975.0,233.8 1005.0,233.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1020.0,311.8 1005.0,337.7 975.0,337.7 960.0,311.8 975.0,285.8 1005.0,285.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1020.0,363.7 1005.0,389.7 975.0,389.7 960.0,363.7 975.0,337.7 1005.0,337.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1020.0,415.7 1005.0,441.7 975.0,441.7 960.0,415.7 975.0,389.7 1005.0,389.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1020.0,467.7 1005.0,493.6 975.0,493.6 960.0,467.7 975.0,441.7 1005.0,441.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1020.0,519.6 1005.0,545.6 975.0,545.6 960.0,519.6 975.0,493.6 1005.0,493.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1020.0,571.6 1005.0,597.6 975.0,597.6 960.0,571.6 975.0,545.6 1005.0,545.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1065.0,-26.0 1050.0,0.0 1020.0,0.0 1005.0,-26.0 1020.0,-52.0 1050.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1065.0,26.0 1050.0,52.0 1020.0,52.0 1005.0,26.0 1020.0,0.0 1050.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1065.0,77.9 1050.0,103.9 1020.0,103.9 1005.0,77.9 1020.0,52.0 1050.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1065.0,129.9 1050.0,155.9 1020.0,155.9 1005.0,129.9 1020.0,103.9 1050.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1065.0,181.9 1050.0,207.8 1020.0,207.8 1005.0,181.9 1020.0,155.9 1050.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1065.0,233.8 1050.0,259.8 1020.0,259.8 1005.0,233.8 1020.0,207.8 1050.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1065.0,285.8 1050.0,311.8 1020.0,311.8 1005.0,285.8 1020.0,259.8 1050.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1065.0,337.7 1050.0,363.7 1020.0,363.7 1005.0,337.7 1020.0,311.8 1050.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1065.0,389.7 1050.0,415.7 1020.0,415.7 1005.0,389.7 1020.0,363.7 1050.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1065.0,441.7 1050.0,467.7 1020.0,467.7 1005.0,441.7 1020.0,415.7 1050.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1065.0,493.6 1050.0,519.6 1020.0,519.6 1005.0,493.6 1020.0,467.7 1050.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1065.0,545.6 1050.0,571.6 1020.0,571.6 1005.0,545.6 1020.0,519.6 1050.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1065.0,597.6 1050.0,623.5 1020.0,623.5 1005.0,597.6 1020.0,571.6 1050.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1110.0,-52.0 1095.0,-26.0 1065.0,-26.0 1050.0,-52.0 1065.0,-77.9 1095.0,-77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1110.0,0.0 1095.0,26.0 1065.0,26.0 1050.0,0.0 1065.0,-26.0 1095.0,-26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1110.0,52.0 1095.0,77.9 1065.0,77.9 1050.0,52.0 1065.0,26.0 1095.0,26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1110.0,103.9 1095.0,129.9 1065.0,129.9 1050.0,103.9 1065.0,77.9 1095.0,77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1110.0,155.9 1095.0,181.9 1065.0,181.9 1050.0,155.9 1065.0,129.9 1095.0,129.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1110.0,207.8 1095.0,233.8 1065.0,233.8 1050.0,207.8 1065.0,181.9 1095.0,181.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1110.0,259.8 1095.0,285.8 1065.0,285.8 1050.0,259.8 1065.0,233.8 1095.0,233.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1110.0,311.8 1095.0,337.7 1065.0,337.7 1050.0,311.8 1065.0,285.8 1095.0,285.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1110.0,363.7 1095.0,389.7 1065.0,389.7 1050.0,363.7 1065.0,337.7 1095.0,337.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1110.0,415.7 1095.0,441.7 1065.0,441.7 1050.0,415.7 1065.0,389.7 1095.0,389.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1110.0,467.7 1095.0,493.6 1065.0,493.6 1050.0,467.7 1065.0,441.7 1095.0,441.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1110.0,519.6 1095.0,545.6 1065.0,545.6 1050.0,519.6 1065.0,493.6 1095.0,493.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1110.0,571.6 1095.0,597.6 1065.0,597.6 1050.0,571.6 1065.0,545.6 1095.0,545.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1155.0,-26.0 1140.0,0.0 1110.0,0.0 1095.0,-26.0 1110.0,-52.0 1140.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1155.0,26.0 1140.0,52.0 1110.0,52.0 1095.0,26.0 1110.0,0.0 1140.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1155.0,77.9 1140.0,103.9 1110.0,103.9 1095.0,77.9 1110.0,52.0 1140.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1155.0,129.9 1140.0,155.9 1110.0,155.9 1095.0,129.9 1110.0,103.9 1140.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1155.0,181.9 1140.0,207.8 1110.0,207.8 1095.0,181.9 1110.0,155.9 1140.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1155.0,233.8 1140.0,259.8 1110.0,259.8 1095.0,233.8 1110.0,207.8 1140.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1155.0,285.8 1140.0,311.8 1110.0,311.8 1095.0,285.8 1110.0,259.8 1140.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1155.0,337.7 1140.0,363.7 1110.0,363.7 1095.0,337.7 1110.0,311.8 1140.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1155.0,389.7 1140.0,415.7 1110.0,415.7 1095.0,389.7 1110.0,363.7 1140.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1155.0,441.7 1140.0,467.7 1110.0,467.7 1095.0,441.7 1110.0,415.7 1140.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1155.0,493.6 1140.0,519.6 1110.0,519.6 1095.0,493.6 1110.0,467.7 1140.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1155.0,545.6 1140.0,571.6 1110.0,571.6 1095.0,545.6 1110.0,519.6 1140.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1155.0,597.6 1140.0,623.5 1110.0,623.5 1095.0,597.6 1110.0,571.6 1140.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1200.0,-52.0 1185.0,-26.0 1155.0,-26.0 1140.0,-52.0 1155.0,-77.9 1185.0,-77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1200.0,0.0 1185.0,26.0 1155.0,26.0 1140.0,0.0 1155.0,-26.0 1185.0,-26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1200.0,52.0 1185.0,77.9 1155.0,77.9 1140.0,52.0 1155.0,26.0 1185.0,26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1200.0,103.9 1185.0,129.9 1155.0,129.9 1140.0,103.9 1155.0,77.9 1185.0,77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1200.0,155.9 1185.0,181.9 1155.0,181.9 1140.0,155.9 1155.0,129.9 1185.0,129.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1200.0,207.8 1185.0,233.8 1155.0,233.8 1140.0,207.8 1155.0,181.9 1185.0,181.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1200.0,259.8 1185.0,285.8 1155.0,285.8 1140.0,259.8 1155.0,233.8 1185.0,233.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1200.0,311.8 1185.0,337.7 1155.0,337.7 1140.0,311.8 1155.0,285.8 1185.0,285.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1200.0,363.7 1185.0,389.7 1155.0,389.7 1140.0,363.7 1155.0,337.7 1185.0,337.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1200.0,415.7 1185.0,441.7 1155.0,441.7 1140.0,415.7 1155.0,389.7 1185.0,389.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1200.0,467.7 1185.0,493.6 1155.0,493.6 1140.0,467.7 1155.0,441.7 1185.0,441.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1200.0,519.6 1185.0,545.6 1155.0,545.6 1140.0,519.6 1155.0,493.6 1185.0,493.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1200.0,571.6 1185.0,597.6 1155.0,597.6 1140.0,571.6 1155.0,545.6 1185.0,545.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1245.0,-26.0 1230.0,0.0 1200.0,0.0 1185.0,-26.0 1200.0,-52.0 1230.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1245.0,26.0 1230.0,52.0 1200.0,52.0 1185.0,26.0 1200.0,0.0 1230.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1245.0,77.9 1230.0,103.9 1200.0,103.9 1185.0,77.9 1200.0,52.0 1230.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1245.0,129.9 1230.0,155.9 1200.0,155.9 1185.0,129.9 1200.0,103.9 1230.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1245.0,181.9 1230.0,207.8 1200.0,207.8 1185.0,181.9 1200.0,155.9 1230.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1245.0,233.8 1230.0,259.8 1200.0,259.8 1185.0,233.8 1200.0,207.8 1230.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1245.0,285.8 1230.0,311.8 1200.0,311.8 1185.0,285.8 1200.0,259.8 1230.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1245.0,337.7 1230.0,363.7 1200.0,363.7 1185.0,337.7 1200.0,311.8 1230.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1245.0,389.7 1230.0,415.7 1200.0,415.7 1185.0,389.7 1200.0,363.7 1230.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1245.0,441.7 1230.0,467.7 1200.0,467.7 1185.0,441.7 1200.0,415.7 1230.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1245.0,493.6 1230.0,519.6 1200.0,519.6 1185.0,493.6 1200.0,467.7 1230.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1245.0,545.6 1230.0,571.6 1200.0,571.6 1185.0,545.6 1200.0,519.6 1230.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1245.0,597.6 1230.0,623.5 1200.0,623.5 1185.0,597.6 1200.0,571.6 1230.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1290.0,-52.0 1275.0,-26.0 1245.0,-26.0 1230.0,-52.0 1245.0,-77.9 1275.0,-77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1290.0,0.0 1275.0,26.0 1245.0,26.0 1230.0,0.0 1245.0,-26.0 1275.0,-26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1290.0,52.0 1275.0,77.9 1245.0,77.9 1230.0,52.0 1245.0,26.0 1275.0,26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1290.0,103.9 1275.0,129.9 1245.0,129.9 1230.0,103.9 1245.0,77.9 1275.0,77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1290.0,155.9 1275.0,181.9 1245.0,181.9 1230.0,155.9 1245.0,129.9 1275.0,129.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1290.0,207.8 1275.0,233.8 1245.0,233.8 1230.0,207.8 1245.0,181.9 1275.0,181.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1290.0,259.8 1275.0,285.8 1245.0,285.8 1230.0,259.8 1245.0,233.8 1275.0,233.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1290.0,311.8 1275.0,337.7 1245.0,337.7 1230.0,311.8 1245.0,285.8 1275.0,285.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1290.0,363.7 1275.0,389.7 1245.0,389.7 1230.0,363.7 1245.0,337.7 1275.0,337.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1290.0,415.7 1275.0,441.7 1245.0,441.7 1230.0,415.7 1245.0,389.7 1275.0,389.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1290.0,467.7 1275.0,493.6 1245.0,493.6 1230.0,467.7 1245.0,441.7 1275.0,441.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1290.0,519.6 1275.0,545.6 1245.0,545.6 1230.0,519.6 1245.0,493.6 1275.0,493.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1290.0,571.6 1275.0,597.6 1245.0,597.6 1230.0,571.6 1245.0,545.6 1275.0,545.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1335.0,-26.0 1320.0,0.0 1290.0,0.0 1275.0,-26.0 1290.0,-52.0 1320.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1335.0,26.0 1320.0,52.0 1290.0,52.0 1275.0,26.0 1290.0,0.0 1320.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1335.0,77.9 1320.0,103.9 1290.0,103.9 1275.0,77.9 1290.0,52.0 1320.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1335.0,129.9 1320.0,155.9 1290.0,155.9 1275.0,129.9 1290.0,103.9 1320.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1335.0,181.9 1320.0,207.8 1290.0,207.8 1275.0,181.9 1290.0,155.9 1320.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1335.0,233.8 1320.0,259.8 1290.0,259.8 1275.0,233.8 1290.0,207.8 1320.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1335.0,285.8 1320.0,311.8 1290.0,311.8 1275.0,285.8 1290.0,259.8 1320.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1335.0,337.7 1320.0,363.7 1290.0,363.7 1275.0,337.7 1290.0,311.8 1320.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1335.0,389.7 1320.0,415.7 1290.0,415.7 1275.0,389.7 1290.0,363.7 1320.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1335.0,441.7 1320.0,467.7 1290.0,467.7 1275.0,441.7 1290.0,415.7 1320.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1335.0,493.6 1320.0,519.6 1290.0,519.6 1275.0,493.6 1290.0,467.7 1320.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1335.0,545.6 1320.0,571.6 1290.0,571.6 1275.0,545.6 1290.0,519.6 1320.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1335.0,597.6 1320.0,623.5 1290.0,623.5 1275.0,597.6 1290.0,571.6 1320.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1380.0,-52.0 1365.0,-26.0 1335.0,-26.0 1320.0,-52.0 1335.0,-77.9 1365.0,-77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1380.0,0.0 1365.0,26.0 1335.0,26.0 1320.0,0.0 1335.0,-26.0 1365.0,-26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1380.0,52.0 1365.0,77.9 1335.0,77.9 1320.0,52.0 1335.0,26.0 1365.0,26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1380.0,103.9 1365.0,129.9 1335.0,129.9 1320.0,103.9 1335.0,77.9 1365.0,77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1380.0,155.9 1365.0,181.9 1335.0,181.9 1320.0,155.9 1335.0,129.9 1365.0,129.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1380.0,207.8 1365.0,233.8 1335.0,233.8 1320.0,207.8 1335.0,181.9 1365.0,181.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1380.0,259.8 1365.0,285.8 1335.0,285.8 1320.0,259.8 1335.0,233.8 1365.0,233.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1380.0,311.8 1365.0,337.7 1335.0,337.7 1320.0,311.8 1335.0,285.8 1365.0,285.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1380.0,363.7 1365.0,389.7 1335.0,389.7 1320.0,363.7 1335.0,337.7 1365.0,337.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1380.0,415.7 1365.0,441.7 1335.0,441.7 1320.0,415.7 1335.0,389.7 1365.0,389.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1380.0,467.7 1365.0,493.6 1335.0,493.6 1320.0,467.7 1335.0,441.7 1365.0,441.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1380.0,519.6 1365.0,545.6 1335.0,545.6 1320.0,519.6 1335.0,493.6 1365.0,493.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1380.0,571.6 1365.0,597.6 1335.0,597.6 1320.0,571.6 1335.0,545.6 1365.0,545.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1425.0,-26.0 1410.0,0.0 1380.0,0.0 1365.0,-26.0 1380.0,-52.0 1410.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1425.0,26.0 1410.0,52.0 1380.0,52.0 1365.0,26.0 1380.0,0.0 1410.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1425.0,77.9 1410.0,103.9 1380.0,103.9 1365.0,77.9 1380.0,52.0 1410.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1425.0,129.9 1410.0,155.9 1380.0,155.9 1365.0,129.9 1380.0,103.9 1410.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1425.0,181.9 1410.0,207.8 1380.0,207.8 1365.0,181.9 1380.0,155.9 1410.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1425.0,233.8 1410.0,259.8 1380.0,259.8 1365.0,233.8 1380.0,207.8 1410.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1425.0,285.8 1410.0,311.8 1380.0,311.8 1365.0,285.8 1380.0,259.8 1410.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1425.0,337.7 1410.0,363.7 1380.0,363.7 1365.0,337.7 1380.0,311.8 1410.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1425.0,389.7 1410.0,415.7 1380.0,415.7 1365.0,389.7 1380.0,363.7 1410.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1425.0,441.7 1410.0,467.7 1380.0,467.7 1365.0,441.7 1380.0,415.7 1410.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1425.0,493.6 1410.0,519.6 1380.0,519.6 1365.0,493.6 1380.0,467.7 1410.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1425.0,545.6 1410.0,571.6 1380.0,571.6 1365.0,545.6 1380.0,519.6 1410.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1425.0,597.6 1410.0,623.5 1380.0,623.5 1365.0,597.6 1380.0,571.6 1410.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1470.0,-52.0 1455.0,-26.0 1425.0,-26.0 1410.0,-52.0 1425.0,-77.9 1455.0,-77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1470.0,0.0 1455.0,26.0 1425.0,26.0 1410.0,0.0 1425.0,-26.0 1455.0,-26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1470.0,52.0 1455.0,77.9 1425.0,77.9 1410.0,52.0 1425.0,26.0 1455.0,26.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1470.0,103.9 1455.0,129.9 1425.0,129.9 1410.0,103.9 1425.0,77.9 1455.0,77.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1470.0,155.9 1455.0,181.9 1425.0,181.9 1410.0,155.9 1425.0,129.9 1455.0,129.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1470.0,207.8 1455.0,233.8 1425.0,233.8 1410.0,207.8 1425.0,181.9 1455.0,181.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1470.0,259.8 1455.0,285.8 1425.0,285.8 1410.0,259.8 1425.0,233.8 1455.0,233.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1470.0,311.8 1455.0,337.7 1425.0,337.7 1410.0,311.8 1425.0,285.8 1455.0,285.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1470.0,363.7 1455.0,389.7 1425.0,389.7 1410.0,363.7 1425.0,337.7 1455.0,337.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1470.0,415.7 1455.0,441.7 1425.0,441.7 1410.0,415.7 1425.0,389.7 1455.0,389.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1470.0,467.7 1455.0,493.6 1425.0,493.6 1410.0,467.7 1425.0,441.7 1455.0,441.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1470.0,519.6 1455.0,545.6 1425.0,545.6 1410.0,519.6 1425.0,493.6 1455.0,493.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1470.0,571.6 1455.0,597.6 1425.0,597.6 1410.0,571.6 1425.0,545.6 1455.0,545.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1515.0,-26.0 1500.0,0.0 1470.0,0.0 1455.0,-26.0 1470.0,-52.0 1500.0,-52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1515.0,26.0 1500.0,52.0 1470.0,52.0 1455.0,26.0 1470.0,0.0 1500.0,0.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1515.0,77.9 1500.0,103.9 1470.0,103.9 1455.0,77.9 1470.0,52.0 1500.0,52.0" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1515.0,129.9 1500.0,155.9 1470.0,155.9 1455.0,129.9 1470.0,103.9 1500.0,103.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1515.0,181.9 1500.0,207.8 1470.0,207.8 1455.0,181.9 1470.0,155.9 1500.0,155.9" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1515.0,233.8 1500.0,259.8 1470.0,259.8 1455.0,233.8 1470.0,207.8 1500.0,207.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1515.0,285.8 1500.0,311.8 1470.0,311.8 1455.0,285.8 1470.0,259.8 1500.0,259.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1515.0,337.7 1500.0,363.7 1470.0,363.7 1455.0,337.7 1470.0,311.8 1500.0,311.8" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1515.0,389.7 1500.0,415.7 1470.0,415.7 1455.0,389.7 1470.0,363.7 1500.0,363.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1515.0,441.7 1500.0,467.7 1470.0,467.7 1455.0,441.7 1470.0,415.7 1500.0,415.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1515.0,493.6 1500.0,519.6 1470.0,519.6 1455.0,493.6 1470.0,467.7 1500.0,467.7" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1515.0,545.6 1500.0,571.6 1470.0,571.6 1455.0,545.6 1470.0,519.6 1500.0,519.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<polygon points="1515.0,597.6 1500.0,623.5 1470.0,623.5 1455.0,597.6 1470.0,571.6 1500.0,571.6" fill="none" stroke="#18344e" stroke-width="0.35" opacity="0.22"/>
<!-- edge glow halos -->
<line x1="100" y1="170" x2="280" y2="90" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="100" y1="170" x2="185" y2="295" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="280" y1="90" x2="385" y2="175" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="185" y1="295" x2="290" y2="390" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="185" y1="295" x2="385" y2="175" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="290" y1="390" x2="400" y2="415" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="385" y1="175" x2="530" y2="265" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="385" y1="175" x2="695" y2="95" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="400" y1="415" x2="530" y2="265" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="400" y1="415" x2="700" y2="415" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="530" y1="265" x2="695" y2="95" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="530" y1="265" x2="700" y2="415" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="530" y1="265" x2="870" y2="240" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="695" y1="95" x2="870" y2="240" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="700" y1="415" x2="870" y2="240" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="870" y1="240" x2="1015" y2="175" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="870" y1="240" x2="1025" y2="405" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="1015" y1="175" x2="1110" y2="90" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="1015" y1="175" x2="1215" y2="295" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="1110" y1="90" x2="1215" y2="295" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="1110" y1="90" x2="1300" y2="200" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="1025" y1="405" x2="1120" y2="390" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="1025" y1="405" x2="1215" y2="295" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="1120" y1="390" x2="1215" y2="295" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<line x1="1215" y1="295" x2="1300" y2="200" stroke="#00aadd" stroke-width="4" opacity="0.06"/>
<!-- edges -->
<line x1="100" y1="170" x2="280" y2="90" stroke="#00ccff" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="100" y1="170" x2="280" y2="90" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="100" y1="170" x2="185" y2="295" stroke="#00bbee" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="100" y1="170" x2="185" y2="295" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="280" y1="90" x2="385" y2="175" stroke="#22ddaa" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="280" y1="90" x2="385" y2="175" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="185" y1="295" x2="290" y2="390" stroke="#00ccff" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="185" y1="295" x2="290" y2="390" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="185" y1="295" x2="385" y2="175" stroke="#00ccff" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="185" y1="295" x2="385" y2="175" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="290" y1="390" x2="400" y2="415" stroke="#00bbee" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="290" y1="390" x2="400" y2="415" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="385" y1="175" x2="530" y2="265" stroke="#22ddaa" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="385" y1="175" x2="530" y2="265" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="385" y1="175" x2="695" y2="95" stroke="#00ccff" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="385" y1="175" x2="695" y2="95" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="400" y1="415" x2="530" y2="265" stroke="#00ccff" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="400" y1="415" x2="530" y2="265" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="400" y1="415" x2="700" y2="415" stroke="#00bbee" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="400" y1="415" x2="700" y2="415" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="530" y1="265" x2="695" y2="95" stroke="#22ddaa" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="530" y1="265" x2="695" y2="95" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="530" y1="265" x2="700" y2="415" stroke="#00ccff" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="530" y1="265" x2="700" y2="415" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="530" y1="265" x2="870" y2="240" stroke="#00ccff" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="530" y1="265" x2="870" y2="240" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="695" y1="95" x2="870" y2="240" stroke="#00bbee" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="695" y1="95" x2="870" y2="240" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="700" y1="415" x2="870" y2="240" stroke="#22ddaa" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="700" y1="415" x2="870" y2="240" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="870" y1="240" x2="1015" y2="175" stroke="#00ccff" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="870" y1="240" x2="1015" y2="175" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="870" y1="240" x2="1025" y2="405" stroke="#00ccff" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="870" y1="240" x2="1025" y2="405" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="1015" y1="175" x2="1110" y2="90" stroke="#00bbee" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="1015" y1="175" x2="1110" y2="90" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="1015" y1="175" x2="1215" y2="295" stroke="#22ddaa" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="1015" y1="175" x2="1215" y2="295" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="1110" y1="90" x2="1215" y2="295" stroke="#00ccff" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="1110" y1="90" x2="1215" y2="295" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="1110" y1="90" x2="1300" y2="200" stroke="#00ccff" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="1110" y1="90" x2="1300" y2="200" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="1025" y1="405" x2="1120" y2="390" stroke="#00bbee" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="1025" y1="405" x2="1120" y2="390" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="1025" y1="405" x2="1215" y2="295" stroke="#22ddaa" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="1025" y1="405" x2="1215" y2="295" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="1120" y1="390" x2="1215" y2="295" stroke="#00ccff" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="1120" y1="390" x2="1215" y2="295" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<line x1="1215" y1="295" x2="1300" y2="200" stroke="#00ccff" stroke-width="0.9" opacity="0.38" filter="url(#lg)"/>
<line x1="1215" y1="295" x2="1300" y2="200" stroke="#ffffff" stroke-width="0.4" opacity="0.12"/>
<!-- relay halos -->
<circle cx="185" cy="295" r="16" fill="none" stroke="#00d4ff" stroke-width="0.6" opacity="0.0">
  <animate attributeName="r" values="8;24;8" dur="4s" begin="0.5s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.25;0;0.25" dur="4s" begin="0.5s" repeatCount="indefinite"/>
</circle>
<circle cx="385" cy="175" r="16" fill="none" stroke="#00d4ff" stroke-width="0.6" opacity="0.0">
  <animate attributeName="r" values="8;24;8" dur="4s" begin="1.1s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.25;0;0.25" dur="4s" begin="1.1s" repeatCount="indefinite"/>
</circle>
<circle cx="400" cy="415" r="16" fill="none" stroke="#00d4ff" stroke-width="0.6" opacity="0.0">
  <animate attributeName="r" values="8;24;8" dur="4s" begin="0.2s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.25;0;0.25" dur="4s" begin="0.2s" repeatCount="indefinite"/>
</circle>
<circle cx="530" cy="265" r="16" fill="none" stroke="#00d4ff" stroke-width="0.6" opacity="0.0">
  <animate attributeName="r" values="8;24;8" dur="4s" begin="1.7s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.25;0;0.25" dur="4s" begin="1.7s" repeatCount="indefinite"/>
</circle>
<circle cx="695" cy="95" r="16" fill="none" stroke="#00d4ff" stroke-width="0.6" opacity="0.0">
  <animate attributeName="r" values="8;24;8" dur="4s" begin="0.8s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.25;0;0.25" dur="4s" begin="0.8s" repeatCount="indefinite"/>
</circle>
<circle cx="700" cy="415" r="16" fill="none" stroke="#00d4ff" stroke-width="0.6" opacity="0.0">
  <animate attributeName="r" values="8;24;8" dur="4s" begin="2.1s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.25;0;0.25" dur="4s" begin="2.1s" repeatCount="indefinite"/>
</circle>
<circle cx="870" cy="240" r="19" fill="none" stroke="#00d4ff" stroke-width="0.6" opacity="0.0">
  <animate attributeName="r" values="11;27;11" dur="4s" begin="0.0s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.25;0;0.25" dur="4s" begin="0.0s" repeatCount="indefinite"/>
</circle>
<circle cx="1015" cy="175" r="16" fill="none" stroke="#00d4ff" stroke-width="0.6" opacity="0.0">
  <animate attributeName="r" values="8;24;8" dur="4s" begin="1.3s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.25;0;0.25" dur="4s" begin="1.3s" repeatCount="indefinite"/>
</circle>
<circle cx="1025" cy="405" r="16" fill="none" stroke="#00d4ff" stroke-width="0.6" opacity="0.0">
  <animate attributeName="r" values="8;24;8" dur="4s" begin="0.6s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.25;0;0.25" dur="4s" begin="0.6s" repeatCount="indefinite"/>
</circle>
<circle cx="1215" cy="295" r="16" fill="none" stroke="#00d4ff" stroke-width="0.6" opacity="0.0">
  <animate attributeName="r" values="8;24;8" dur="4s" begin="1.9s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.25;0;0.25" dur="4s" begin="1.9s" repeatCount="indefinite"/>
</circle>
<!-- relay nodes -->
<circle cx="185" cy="295" r="7" fill="url(#rGrad)" filter="url(#cg)">
  <animate attributeName="r" values="6;9;6" dur="3.6s" begin="0.5s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.65;1;0.65" dur="3.6s" begin="0.5s" repeatCount="indefinite"/>
</circle>
<circle cx="385" cy="175" r="7" fill="url(#rGrad)" filter="url(#cg)">
  <animate attributeName="r" values="6;9;6" dur="3.8s" begin="1.1s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.65;1;0.65" dur="3.8s" begin="1.1s" repeatCount="indefinite"/>
</circle>
<circle cx="400" cy="415" r="7" fill="url(#rGrad)" filter="url(#cg)">
  <animate attributeName="r" values="6;9;6" dur="3.6s" begin="0.2s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.65;1;0.65" dur="3.6s" begin="0.2s" repeatCount="indefinite"/>
</circle>
<circle cx="530" cy="265" r="7" fill="url(#rGrad)" filter="url(#cg)">
  <animate attributeName="r" values="6;9;6" dur="4.0s" begin="1.7s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.65;1;0.65" dur="4.0s" begin="1.7s" repeatCount="indefinite"/>
</circle>
<circle cx="695" cy="95" r="7" fill="url(#rGrad)" filter="url(#cg)">
  <animate attributeName="r" values="6;9;6" dur="3.7s" begin="0.8s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.65;1;0.65" dur="3.7s" begin="0.8s" repeatCount="indefinite"/>
</circle>
<circle cx="700" cy="415" r="7" fill="url(#rGrad)" filter="url(#cg)">
  <animate attributeName="r" values="6;9;6" dur="4.1s" begin="2.1s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.65;1;0.65" dur="4.1s" begin="2.1s" repeatCount="indefinite"/>
</circle>
<circle cx="870" cy="240" r="10" fill="url(#rGrad)" filter="url(#cg)">
  <animate attributeName="r" values="9;12;9" dur="3.2s" begin="0.0s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.65;1;0.65" dur="3.2s" begin="0.0s" repeatCount="indefinite"/>
</circle>
<circle cx="1015" cy="175" r="7" fill="url(#rGrad)" filter="url(#cg)">
  <animate attributeName="r" values="6;9;6" dur="3.9s" begin="1.3s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.65;1;0.65" dur="3.9s" begin="1.3s" repeatCount="indefinite"/>
</circle>
<circle cx="1025" cy="405" r="7" fill="url(#rGrad)" filter="url(#cg)">
  <animate attributeName="r" values="6;9;6" dur="3.7s" begin="0.6s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.65;1;0.65" dur="3.7s" begin="0.6s" repeatCount="indefinite"/>
</circle>
<circle cx="1215" cy="295" r="7" fill="url(#rGrad)" filter="url(#cg)">
  <animate attributeName="r" values="6;9;6" dur="4.1s" begin="1.9s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.65;1;0.65" dur="4.1s" begin="1.9s" repeatCount="indefinite"/>
</circle>
<!-- agent halos -->
<circle cx="100" cy="170" r="35" fill="none" stroke="#3ddc84" stroke-width="0.8" opacity="0">
  <animate attributeName="r" values="27;45;27" dur="3.0s" begin="0.0s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.35;0;0.35" dur="3.0s" begin="0.0s" repeatCount="indefinite"/>
</circle>
<circle cx="100" cy="170" r="55" fill="none" stroke="#3ddc84" stroke-width="0.8" opacity="0">
  <animate attributeName="r" values="47;65;47" dur="3.0s" begin="0.5s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.35;0;0.35" dur="3.0s" begin="0.5s" repeatCount="indefinite"/>
</circle>
<circle cx="280" cy="90" r="35" fill="none" stroke="#3ddc84" stroke-width="0.8" opacity="0">
  <animate attributeName="r" values="27;45;27" dur="3.0s" begin="0.6s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.35;0;0.35" dur="3.0s" begin="0.6s" repeatCount="indefinite"/>
</circle>
<circle cx="280" cy="90" r="55" fill="none" stroke="#3ddc84" stroke-width="0.8" opacity="0">
  <animate attributeName="r" values="47;65;47" dur="3.0s" begin="1.1s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.35;0;0.35" dur="3.0s" begin="1.1s" repeatCount="indefinite"/>
</circle>
<circle cx="290" cy="390" r="35" fill="none" stroke="#3ddc84" stroke-width="0.8" opacity="0">
  <animate attributeName="r" values="27;45;27" dur="3.0s" begin="1.2s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.35;0;0.35" dur="3.0s" begin="1.2s" repeatCount="indefinite"/>
</circle>
<circle cx="290" cy="390" r="55" fill="none" stroke="#3ddc84" stroke-width="0.8" opacity="0">
  <animate attributeName="r" values="47;65;47" dur="3.0s" begin="1.7s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.35;0;0.35" dur="3.0s" begin="1.7s" repeatCount="indefinite"/>
</circle>
<circle cx="1110" cy="90" r="35" fill="none" stroke="#3ddc84" stroke-width="0.8" opacity="0">
  <animate attributeName="r" values="27;45;27" dur="3.0s" begin="0.3s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.35;0;0.35" dur="3.0s" begin="0.3s" repeatCount="indefinite"/>
</circle>
<circle cx="1110" cy="90" r="55" fill="none" stroke="#3ddc84" stroke-width="0.8" opacity="0">
  <animate attributeName="r" values="47;65;47" dur="3.0s" begin="0.8s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.35;0;0.35" dur="3.0s" begin="0.8s" repeatCount="indefinite"/>
</circle>
<circle cx="1120" cy="390" r="35" fill="none" stroke="#3ddc84" stroke-width="0.8" opacity="0">
  <animate attributeName="r" values="27;45;27" dur="3.0s" begin="0.9s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.35;0;0.35" dur="3.0s" begin="0.9s" repeatCount="indefinite"/>
</circle>
<circle cx="1120" cy="390" r="55" fill="none" stroke="#3ddc84" stroke-width="0.8" opacity="0">
  <animate attributeName="r" values="47;65;47" dur="3.0s" begin="1.4s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.35;0;0.35" dur="3.0s" begin="1.4s" repeatCount="indefinite"/>
</circle>
<circle cx="1300" cy="200" r="35" fill="none" stroke="#3ddc84" stroke-width="0.8" opacity="0">
  <animate attributeName="r" values="27;45;27" dur="3.0s" begin="1.5s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.35;0;0.35" dur="3.0s" begin="1.5s" repeatCount="indefinite"/>
</circle>
<circle cx="1300" cy="200" r="55" fill="none" stroke="#3ddc84" stroke-width="0.8" opacity="0">
  <animate attributeName="r" values="47;65;47" dur="3.0s" begin="2.0s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.35;0;0.35" dur="3.0s" begin="2.0s" repeatCount="indefinite"/>
</circle>
<!-- agent nodes -->
<circle cx="100" cy="170" r="24" fill="url(#aGrad)" opacity="0.15" filter="url(#ag)"/>
<circle cx="100" cy="170" r="22" fill="url(#aGrad)" filter="url(#ag)">
  <animate attributeName="r" values="20;23;20" dur="3s" begin="0.0s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.85;1;0.85" dur="3s" begin="0.0s" repeatCount="indefinite"/>
</circle>
<use href="#ph" x="100" y="170" width="30" height="52" color="#3ddc84" transform="translate(100,170) scale(1)"/>
<circle cx="280" cy="90" r="24" fill="url(#aGrad)" opacity="0.15" filter="url(#ag)"/>
<circle cx="280" cy="90" r="22" fill="url(#aGrad)" filter="url(#ag)">
  <animate attributeName="r" values="20;23;20" dur="3s" begin="0.6s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.85;1;0.85" dur="3s" begin="0.6s" repeatCount="indefinite"/>
</circle>
<use href="#ph" x="280" y="90" width="30" height="52" color="#3ddc84" transform="translate(280,90) scale(1)"/>
<circle cx="290" cy="390" r="24" fill="url(#aGrad)" opacity="0.15" filter="url(#ag)"/>
<circle cx="290" cy="390" r="22" fill="url(#aGrad)" filter="url(#ag)">
  <animate attributeName="r" values="20;23;20" dur="3s" begin="1.2s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.85;1;0.85" dur="3s" begin="1.2s" repeatCount="indefinite"/>
</circle>
<use href="#ph" x="290" y="390" width="30" height="52" color="#3ddc84" transform="translate(290,390) scale(1)"/>
<circle cx="1110" cy="90" r="24" fill="url(#aGrad)" opacity="0.15" filter="url(#ag)"/>
<circle cx="1110" cy="90" r="22" fill="url(#aGrad)" filter="url(#ag)">
  <animate attributeName="r" values="20;23;20" dur="3s" begin="0.3s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.85;1;0.85" dur="3s" begin="0.3s" repeatCount="indefinite"/>
</circle>
<use href="#ph" x="1110" y="90" width="30" height="52" color="#3ddc84" transform="translate(1110,90) scale(1)"/>
<circle cx="1120" cy="390" r="24" fill="url(#aGrad)" opacity="0.15" filter="url(#ag)"/>
<circle cx="1120" cy="390" r="22" fill="url(#aGrad)" filter="url(#ag)">
  <animate attributeName="r" values="20;23;20" dur="3s" begin="0.9s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.85;1;0.85" dur="3s" begin="0.9s" repeatCount="indefinite"/>
</circle>
<use href="#ph" x="1120" y="390" width="30" height="52" color="#3ddc84" transform="translate(1120,390) scale(1)"/>
<circle cx="1300" cy="200" r="24" fill="url(#aGrad)" opacity="0.15" filter="url(#ag)"/>
<circle cx="1300" cy="200" r="22" fill="url(#aGrad)" filter="url(#ag)">
  <animate attributeName="r" values="20;23;20" dur="3s" begin="1.5s" repeatCount="indefinite"/>
  <animate attributeName="opacity" values="0.85;1;0.85" dur="3s" begin="1.5s" repeatCount="indefinite"/>
</circle>
<use href="#ph" x="1300" y="200" width="30" height="52" color="#3ddc84" transform="translate(1300,200) scale(1)"/>
<!-- data particles -->
<circle r="3" fill="#00ffcc" filter="url(#pg)" opacity="0.9">
  <animateMotion dur="3.5s" begin="0.0s" repeatCount="indefinite">
    <mpath href="#fp1"/>
  </animateMotion>
  <animate attributeName="opacity" values="0;1;1;0" dur="3.5s" begin="0.0s" repeatCount="indefinite"/>
</circle>
<circle r="3" fill="#00ffcc" filter="url(#pg)" opacity="0.9">
  <animateMotion dur="2.8s" begin="0.4s" repeatCount="indefinite">
    <mpath href="#fp2"/>
  </animateMotion>
  <animate attributeName="opacity" values="0;1;1;0" dur="2.8s" begin="0.4s" repeatCount="indefinite"/>
</circle>
<circle r="3" fill="#7df9ff" filter="url(#pg)" opacity="0.9">
  <animateMotion dur="4.0s" begin="1.0s" repeatCount="indefinite">
    <mpath href="#fp3"/>
  </animateMotion>
  <animate attributeName="opacity" values="0;1;1;0" dur="4.0s" begin="1.0s" repeatCount="indefinite"/>
</circle>
<circle r="3" fill="#00ffcc" filter="url(#pg)" opacity="0.9">
  <animateMotion dur="3.2s" begin="0.2s" repeatCount="indefinite">
    <mpath href="#fp4"/>
  </animateMotion>
  <animate attributeName="opacity" values="0;1;1;0" dur="3.2s" begin="0.2s" repeatCount="indefinite"/>
</circle>
<circle r="3" fill="#7df9ff" filter="url(#pg)" opacity="0.9">
  <animateMotion dur="3.0s" begin="0.7s" repeatCount="indefinite">
    <mpath href="#fp5"/>
  </animateMotion>
  <animate attributeName="opacity" values="0;1;1;0" dur="3.0s" begin="0.7s" repeatCount="indefinite"/>
</circle>
<circle r="3" fill="#00ffcc" filter="url(#pg)" opacity="0.9">
  <animateMotion dur="2.5s" begin="1.5s" repeatCount="indefinite">
    <mpath href="#fp6"/>
  </animateMotion>
  <animate attributeName="opacity" values="0;1;1;0" dur="2.5s" begin="1.5s" repeatCount="indefinite"/>
</circle>
<circle r="3" fill="#7df9ff" filter="url(#pg)" opacity="0.9">
  <animateMotion dur="3.8s" begin="0.9s" repeatCount="indefinite">
    <mpath href="#fp7"/>
  </animateMotion>
  <animate attributeName="opacity" values="0;1;1;0" dur="3.8s" begin="0.9s" repeatCount="indefinite"/>
</circle>
<circle r="3" fill="#00ffcc" filter="url(#pg)" opacity="0.9">
  <animateMotion dur="4.2s" begin="0.3s" repeatCount="indefinite">
    <mpath href="#fp8"/>
  </animateMotion>
  <animate attributeName="opacity" values="0;1;1;0" dur="4.2s" begin="0.3s" repeatCount="indefinite"/>
</circle>
<circle r="3" fill="#ffffff" filter="url(#pg)" opacity="0.9">
  <animateMotion dur="4.0s" begin="2.0s" repeatCount="indefinite">
    <mpath href="#fp3"/>
  </animateMotion>
  <animate attributeName="opacity" values="0;1;1;0" dur="4.0s" begin="2.0s" repeatCount="indefinite"/>
</circle>
<circle r="3" fill="#7df9ff" filter="url(#pg)" opacity="0.9">
  <animateMotion dur="3.5s" begin="1.8s" repeatCount="indefinite">
    <mpath href="#fp1"/>
  </animateMotion>
  <animate attributeName="opacity" values="0;1;1;0" dur="3.5s" begin="1.8s" repeatCount="indefinite"/>
</circle>
<!-- border accent lines -->
<line x1="0" y1="2" x2="1400" y2="2" stroke="url(#borderGrad)" stroke-width="1.5" opacity="0.8"/>
<line x1="0" y1="498" x2="1400" y2="498" stroke="url(#borderGradB)" stroke-width="1.5" opacity="0.6"/>
<line x1="30" y1="2" x2="30" y2="22" stroke="#3ddc84" stroke-width="1.2" opacity="0.5"/>
<line x1="30" y1="478" x2="30" y2="498" stroke="#00d4ff" stroke-width="1.2" opacity="0.4"/>
<line x1="1370" y1="2" x2="1370" y2="22" stroke="#3ddc84" stroke-width="1.2" opacity="0.5"/>
<line x1="1370" y1="478" x2="1370" y2="498" stroke="#00d4ff" stroke-width="1.2" opacity="0.4"/>
<!-- title -->
<text x="700" y="235" font-family="'Courier New', monospace" font-size="96" font-weight="900" text-anchor="middle" fill="#00d4ff" opacity="0.18" filter="url(#tg)" letter-spacing="28">MESH AI</text>
<text x="700" y="235" font-family="'Courier New', monospace" font-size="96" font-weight="900" text-anchor="middle" fill="#ffffff" filter="url(#tg)" letter-spacing="28">MESH AI</text>
<!-- subtitle -->
<text x="700" y="283" font-family="'Courier New', monospace" font-size="14.5" text-anchor="middle" fill="#00d4ff" opacity="0.85" filter="url(#stg)" letter-spacing="4">DECENTRALIZED AUTONOMOUS AI AGENTS  ·  MULTI-RADIO MESH NETWORKING</text>
<!-- bottom labels -->
<text x="700" y="466" font-family="'Courier New', monospace" font-size="10" text-anchor="middle" fill="#2a8a5a" opacity="0.6" letter-spacing="3">KOTLIN  ·  ANDROID 12+  ·  GEMINI NANO / GEMMA 2B  ·  MESHRABIYA  ·  BLE GATT  ·  NOISE PROTOCOL  ·  HILT  ·  JETPACK COMPOSE</text>
<text x="100" y="195" font-family="'Courier New', monospace" font-size="9" text-anchor="middle" fill="#3ddc84" opacity="0.55" letter-spacing="1">NODE-1</text>
<text x="280" y="115" font-family="'Courier New', monospace" font-size="9" text-anchor="middle" fill="#3ddc84" opacity="0.55" letter-spacing="1">NODE-2</text>
<text x="290" y="365" font-family="'Courier New', monospace" font-size="9" text-anchor="middle" fill="#3ddc84" opacity="0.55" letter-spacing="1">NODE-3</text>
<text x="1110" y="115" font-family="'Courier New', monospace" font-size="9" text-anchor="middle" fill="#3ddc84" opacity="0.55" letter-spacing="1">NODE-4</text>
<text x="1120" y="365" font-family="'Courier New', monospace" font-size="9" text-anchor="middle" fill="#3ddc84" opacity="0.55" letter-spacing="1">NODE-5</text>
<text x="1300" y="225" font-family="'Courier New', monospace" font-size="9" text-anchor="middle" fill="#3ddc84" opacity="0.55" letter-spacing="1">NODE-6</text>
<text x="870" y="215" font-family="'Courier New', monospace" font-size="8.5" 
  text-anchor="middle" fill="#00d4ff" opacity="0.5" letter-spacing="1">REACT LOOP</text>
<text x="870" y="226" font-family="'Courier New', monospace" font-size="7.5" 
  text-anchor="middle" fill="#00aacc" opacity="0.4" letter-spacing="0.5">THINK · ACT · OBSERVE</text>
<rect x="18" y="16" width="115" height="22" rx="3" fill="#0a2a18" stroke="#3ddc84" stroke-width="0.8" opacity="0.7"/>
<text x="75" y="31" font-family="'Courier New', monospace" font-size="9.5" text-anchor="middle" 
  fill="#3ddc84" opacity="0.85" letter-spacing="1">6 AGENT NODES LIVE</text>
<rect x="1267" y="16" width="115" height="22" rx="3" fill="#071e30" stroke="#00d4ff" stroke-width="0.8" opacity="0.7"/>
<text x="1325" y="31" font-family="'Courier New', monospace" font-size="9.5" text-anchor="middle" 
  fill="#00d4ff" opacity="0.85" letter-spacing="1">MESH ENCRYPTED</text>
</svg>
# 🕸️ MeshAI

### Decentralized Autonomous AI Agents with Multi-Radio Mesh Networking

> **Every Android phone is a node. Every node is an agent. The mesh never sleeps.**

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-16%2B-brightgreen)](https://developer.android.com)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-31-blue)](https://developer.android.com/about/versions/12)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple)](https://kotlinlang.org)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-blue)](https://developer.android.com/about/versions/16)

---

## Overview

MeshAI transforms Android devices into **autonomous AI agent nodes** that self-organize into a resilient, multi-radio mesh network — no internet, no central server, no cloud. Phones communicate with phones, run on-device LLMs, decompose goals into tasks, execute tools, and collaborate across the mesh 24/7.

### Why Android-only?

iOS sandboxing prohibits background Wi-Fi Direct sockets, persistent BLE GATT servers, background peer-to-peer socket listeners, and foreground service semantics for mesh networking. Apple explicitly blocks the low-level APIs required for true mesh. **Android is the only mobile platform where this architecture is possible.**

---

## Features

### 🌐 Multi-Radio Mesh Networking

Three radio layers form an adaptive hybrid transport:

| Layer | Technology | Use Case |
|---|---|---|
| **Tier 1** | Meshrabiya (Wi-Fi Direct multi-hop) | High-bandwidth multi-hop routing, virtual IPs, TCP/UDP |
| **Tier 2** | Google Nearby Connections (BLE + Wi-Fi) | Peer discovery, cross-device data relay |
| **Tier 3** | BLE GATT | Ultra-low-power presence beaconing, small payloads |

The network stack auto-selects the best available transport per peer. Mesh → Nearby → Cellular/Wi-Fi fallback is handled transparently. All mesh payloads are encrypted using the **Noise protocol**.

### 🤖 Autonomous Agent Core

The agent runtime is built on three cooperating subsystems:

**`ReActLoop`** — The heart of each node. Runs a Reasoning + Acting loop capped at 12 steps:
```
Thought → Action (tool call) → Observation → Thought → ... → FINAL ANSWER
```
- Builds structured context per step (task, memory, available tools, node capabilities, battery, owner presence)
- Parses `Action: <tool>` / `Action Input: <json>` from LLM output
- Persists each step's observation to `AgentMemory` for cross-session recall
- Exposes `loopState: StateFlow<LoopState>` for UI binding

**`GoalEngine`** — Decomposes free-text user goals into typed `AgentTask` objects via LLM. Supported task types:

`SEND_SMS` · `ANSWER_CALL` · `TAKE_PHOTO` · `GET_LOCATION` · `MONITOR` · `RESPOND_TO_MESSAGE` · `LLM_REASONING` · `DELEGATE` · `CUSTOM`

Falls back to a single `CUSTOM` task if the LLM returns unparseable JSON.

**`OwnerPresenceDetector`** — Polls every 60 seconds to determine whether the owner is available. Owner is declared **unavailable** when any of the following are true:
- Screen has been off for > 30 minutes (persisted via DataStore across reboots)
- Do Not Disturb is in `INTERRUPTION_FILTER_NONE` or `INTERRUPTION_FILTER_ALARMS` state
- User has explicitly toggled **Agent Mode** from the dashboard

When unavailable, the `ReActLoop` becomes fully proactive — answering messages, screening calls, and completing queued tasks autonomously.

### 🧠 On-Device LLM Engine

`LlmEngine` implements tiered inference with automatic fallback:

| Tier | Backend | Requirement |
|---|---|---|
| **1 — Gemini Nano** | Android AICore (ML Kit GenAI) | Pixel 8+ / Android 16+ AICore device |
| **2 — Gemma 2B** | MediaPipe LLM Inference | Any Android 12+ device with model file |
| **3 — Degraded** | Graceful no-op response | Fallback when no model is available |

Gemma prompt format uses the standard `<start_of_turn>` / `<end_of_turn>` instruction template. Model file: `gemma2b-it-cpu-int4.bin` placed in the app's external files directory.

> **Gemini Nano binding:** The AICore stub is present but disabled pending stable production API surface. See the inline comments in `LlmEngine.kt` for the binding pattern to follow when deploying on Pixel 8+.

### 🛠️ Device Tool Use

The `ToolRegistry` dispatches tool calls from the `ReActLoop` to registered tool implementations:

| Tool | File | Android API |
|---|---|---|
| Send SMS | `SmsTool.kt` | `SmsManager` |
| Call screening | `CallTool.kt` | `CallScreeningService`, `ConnectionService` |
| Camera capture | `CameraTool.kt` | CameraX |
| GPS location | `LocationTool.kt` | Fused Location Provider |
| Notification R/W | `NotificationTool.kt` | `NotificationListenerService` |

Tools return a typed `ToolResult` with a `.summary` string consumed by the `ReActLoop` as the observation.

### 🔒 Security Model

| Layer | Mechanism |
|---|---|
| Mesh transport | Noise protocol (session encryption per peer) |
| Local storage | Jetpack Security · AES-256-GCM via `EncryptedSharedPreferences` / `EncryptedFile` |
| Cross-node key exchange | Age encryption |
| Permissions | Gated at runtime with graceful degradation on denial |

### ⚙️ Lifecycle & Persistence

- **`AgentForegroundService`** — Persistent foreground service providing the coroutine scope for all agent loops. Survives app background and system memory pressure.
- **`AgentWorker`** — WorkManager `CoroutineWorker` for deferrable, constraint-aware background tasks.
- **`BootReceiver`** — `BOOT_COMPLETED` / `QUICKBOOT_POWERON` receiver auto-restarts the agent service after device reboot.
- **`MeshAIDatabase`** — Room database backing `AgentMemory`, `AgentTask`, and `AgentNode` persistence.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                             UI Layer                                 │
│   Jetpack Compose · Material 3 · DashboardScreen · MeshAINavGraph   │
├─────────────────────────────────────────────────────────────────────┤
│                         Agent Core Layer                             │
│        ReActLoop · GoalEngine · AgentMemory · OwnerPresenceDetector  │
├────────────────┬───────────────────────────┬────────────────────────┤
│   LLM Layer    │        Tool Layer          │      Mesh Layer        │
│                │                            │                        │
│  LlmEngine     │  ToolRegistry              │  MeshNetwork           │
│  ├ Gemini Nano │  ├ SmsTool                 │  ├ MeshrabiyaLayer     │
│  ├ Gemma 2B    │  ├ CallTool                │  ├ NearbyLayer         │
│  └ Degraded    │  ├ CameraTool              │  └ BleGattLayer        │
│                │  ├ LocationTool            │                        │
│                │  └ NotificationTool        │  MeshEncryption        │
│                │                            │  MeshMessage           │
├────────────────┴───────────────────────────┴────────────────────────┤
│                       Infrastructure Layer                           │
│  Room (MeshAIDatabase) · DataStore · Hilt (AppModule)               │
│  Coroutines · Flow · WorkManager · ForegroundService · BootReceiver  │
└─────────────────────────────────────────────────────────────────────┘
```

**Dependency injection** is handled entirely by Hilt. `AppModule` provides the coroutine scope, DataStore instance, and all singleton bindings. `LlmEngine`, `ReActLoop`, `GoalEngine`, `ToolRegistry`, and `OwnerPresenceDetector` are all `@Singleton` and injected throughout.

---

## Project Structure

```
MeshAI/
├── agent/
│   ├── AgentNode.kt               # Node identity, capabilities, battery, owner state
│   ├── AgentTask.kt               # Task model: title, description, TaskType, TaskPriority
│   ├── AgentMemory.kt             # Short-term + persistent key-value memory
│   ├── AgentRepository.kt         # Repository layer for Room + DataStore
│   ├── AgentWorker.kt             # WorkManager CoroutineWorker
│   ├── AgentForegroundService.kt  # Persistent foreground service + coroutine scope
│   ├── ReActLoop.kt               # Core Thought→Action→Observation reasoning engine
│   ├── GoalEngine.kt              # LLM-based goal decomposition → AgentTask list
│   └── OwnerPresenceDetector.kt   # Screen-off / DND / Agent Mode detection
│
├── llm/
│   └── LlmEngine.kt               # Tiered inference: Gemini Nano → Gemma → Degraded
│
├── mesh/
│   ├── MeshNetwork.kt             # Unified mesh abstraction & routing
│   ├── MeshMessage.kt             # Encrypted message data model
│   ├── MeshEncryption.kt          # Noise protocol session encryption
│   ├── MeshrabiyaLayer.kt         # Wi-Fi Direct multi-hop mesh via Meshrabiya
│   ├── NearbyLayer.kt             # Google Nearby Connections BLE+Wi-Fi layer
│   └── BleGattLayer.kt            # BLE GATT low-power discovery & beaconing
│
├── tools/
│   ├── ToolRegistry.kt            # Tool registration + dispatch for ReActLoop
│   ├── SmsTool.kt                 # SmsManager send/receive
│   ├── CallTool.kt                # CallScreeningService + ConnectionService
│   ├── CameraTool.kt              # CameraX photo capture
│   ├── LocationTool.kt            # Fused Location Provider GPS
│   └── NotificationTool.kt        # NotificationListenerService R/W
│
├── data/
│   ├── MeshAIDatabase.kt          # Room database definition
│   └── AppModule.kt               # Hilt DI module
│
├── ui/
│   ├── DashboardScreen.kt         # Compose mesh map + agent status dashboard
│   ├── DashboardViewModel.kt      # ViewModel for dashboard state
│   ├── OtherScreens.kt            # Task queue, settings, node detail screens
│   ├── MeshAINavGraph.kt          # Compose navigation graph
│   ├── Theme.kt                   # Material 3 colour scheme
│   └── Typography.kt              # Type scale
│
├── MainActivity.kt
├── MeshAIApp.kt                   # Application class + Hilt entry point
├── BootReceiver.kt                # Auto-restart on BOOT_COMPLETED
├── AndroidManifest.xml
│
└── test/
    ├── ReActLoopTest.kt           # Unit tests for the reasoning loop
    └── MeshEncryptionTest.kt      # Unit tests for Noise protocol encryption
```

---

## Setup

### Prerequisites

- **Android Studio** Ladybug 2024.2+ or Meerkat 2025.1+
- **Android device** running Android 12+ (SDK 31); Android 16 recommended for full feature set
- **Wi-Fi Direct** support required for Meshrabiya mesh features
- **Gemini Nano** requires Pixel 8+ or a device with AICore; other devices use the Gemma MediaPipe fallback

### Build & Run

```bash
git clone https://github.com/stackbleed-ctrl/MeshAI.git
cd MeshAI
# Open in Android Studio, sync Gradle, then:
./gradlew assembleDebug
./gradlew installDebug
```

### Gemma Model Setup

The Gemma fallback requires the model file to be pushed to the device:

```bash
# Download gemma2b-it-cpu-int4.bin from https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference
adb push gemma2b-it-cpu-int4.bin \
  /sdcard/Android/data/com.meshai/files/gemma2b-it-cpu-int4.bin
```

The `LlmEngine` reads from `context.getExternalFilesDir(null)` — no root required.

### Required Permissions

All permissions are requested at runtime. Some require manual Settings grants:

| Permission | Grant Method | Used By |
|---|---|---|
| `SEND_SMS`, `RECEIVE_SMS` | Runtime dialog | `SmsTool` |
| `MANAGE_OWN_CALLS` | Runtime dialog | `CallTool` |
| `ACCESS_FINE_LOCATION` | Runtime dialog | `LocationTool` |
| `CAMERA` | Runtime dialog | `CameraTool` |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Settings → Notifications → App Access | `NotificationTool` |
| `BIND_CALL_SCREENING_SERVICE` | Settings → Phone → Call Screening | `CallTool` |
| `NEARBY_WIFI_DEVICES` | Runtime dialog | `NearbyLayer` |
| `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT` | Runtime dialog | `BleGattLayer` |

Agent Mode and all tool use require explicit user consent. The app gracefully degrades if any permission is denied.

---

## Running Tests

```bash
# Unit tests (ReActLoopTest, MeshEncryptionTest)
./gradlew test

# Instrumented tests on device
./gradlew connectedAndroidTest
```

---

## Key Design Decisions

**Why Meshrabiya over Wi-Fi Direct raw sockets?**
Meshrabiya provides true multi-hop routing with virtual IP assignment and TCP/UDP socket support over Wi-Fi Direct, removing the 2-device limit and enabling a proper mesh topology.

**Why a tiered LLM strategy?**
On-device inference is hardware-dependent. Graceful degradation means MeshAI is functional on any Android 12+ device even without a downloaded model — it reports inability and queues the task rather than crashing.

**Why ReAct over a simpler prompt loop?**
The Thought/Action/Observation structure forces the model to reason explicitly before acting, produces auditable step-by-step traces stored in `AgentMemory`, and naturally handles multi-step tasks (monitor → detect → alert → confirm) with tool use at each step.

**Why `ForegroundService` + `WorkManager` together?**
The foreground service provides a persistent coroutine scope and keeps the mesh connections alive while the owner is away. WorkManager handles constraint-aware deferrable tasks (e.g. "check inventory when on Wi-Fi") with guaranteed execution even after process death.

---

## Roadmap

- [ ] Gemini Nano AICore binding (pending stable Android 16 GA API surface)
- [ ] Mesh topology map visualization in `DashboardScreen`
- [ ] Cross-node task delegation via `MeshNetwork` (`DELEGATE` task type)
- [ ] Shared encrypted mesh knowledge base (synchronized `AgentMemory` across nodes)
- [ ] Wake-word detection via `AudioRecord` for hands-free agent activation
- [ ] CI/CD pipeline with GitHub Actions (build, lint, unit tests)
- [ ] APK release artifact via GitHub Releases

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). PRs welcome — especially for Gemini Nano AICore binding, Meshrabiya topology improvements, and new `ToolRegistry` implementations.

---

## Disclaimer

MeshAI is a research and experimental project. Autonomous device control — including call answering, SMS sending, and notification access — requires explicit user consent at every step. Always comply with local telecommunications laws regarding automated communications and recording. The authors are not responsible for misuse.

---

## License

MIT License — see [LICENSE](LICENSE).

---

*Built with Kotlin · Jetpack Compose · Hilt · Room · MediaPipe · Meshrabiya · Noise Protocol*
