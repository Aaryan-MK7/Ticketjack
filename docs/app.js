const STORAGE_KEY = 'ticketjack-interactive-demo-v1';

const seedState = () => ({
  version: 1,
  drawCounter: 0,
  users: [
    { id: 'maya', name: 'Maya Chen', role: 'entrant', active: true, initials: 'MC' },
    { id: 'noah', name: 'Noah Williams', role: 'entrant', active: true, initials: 'NW' },
    { id: 'amina', name: 'Amina Yusuf', role: 'entrant', active: true, initials: 'AY' },
    { id: 'leo', name: 'Leo Martin', role: 'entrant', active: true, initials: 'LM' },
    { id: 'sora', name: 'Sora Kim', role: 'entrant', active: true, initials: 'SK' },
    { id: 'priya', name: 'Priya Shah', role: 'entrant', active: true, initials: 'PS' },
    { id: 'jordan', name: 'Jordan Lee', role: 'organizer', active: true, initials: 'JL' },
    { id: 'admin', name: 'Alex Morgan', role: 'admin', active: true, initials: 'AM' },
  ],
  events: [
    {
      id: 'river',
      name: 'River Valley Sessions',
      category: 'Music',
      date: 'Fri, Nov 6 · 7:00 PM',
      location: 'Louise McKinney Park',
      organizerId: 'jordan',
      capacity: 4,
      status: 'open',
      active: true,
      icon: '♫',
      color: '#8db8f5',
      description: 'An outdoor evening of local music, food, and river-valley views.',
      waitlist: ['maya', 'noah', 'amina', 'leo', 'sora', 'priya'],
      selected: {},
      confirmed: [],
    },
    {
      id: 'design',
      name: 'Design Systems Lab',
      category: 'Workshop',
      date: 'Tue, Nov 10 · 5:30 PM',
      location: 'Enterprise Square',
      organizerId: 'jordan',
      capacity: 3,
      status: 'open',
      active: true,
      icon: '✦',
      color: '#83f49d',
      description: 'A practical workshop for building accessible, reusable product foundations.',
      waitlist: ['noah', 'sora'],
      selected: {},
      confirmed: [],
    },
    {
      id: 'winter',
      name: 'Winter Light Market',
      category: 'Community',
      date: 'Sat, Nov 21 · 3:00 PM',
      location: 'Old Strathcona',
      organizerId: 'jordan',
      capacity: 5,
      status: 'open',
      active: true,
      icon: '◌',
      color: '#ffd85a',
      description: 'Local makers, warm drinks, live demonstrations, and a fair ticket draw.',
      waitlist: ['amina', 'leo', 'priya'],
      selected: {},
      confirmed: [],
    },
  ],
  notifications: [
    {
      id: 'n1',
      userId: 'maya',
      title: 'Welcome to Ticketjack',
      detail: 'Browse open events and join any waiting list.',
      time: 'Just now',
    },
  ],
  audit: [
    {
      id: 'a1',
      title: 'Demo data created',
      detail: 'Seeded roles, events, and waiting lists are ready.',
      time: 'Today',
    },
  ],
});

const roleProfiles = {
  entrant: {
    userId: 'maya',
    description: 'Discover events, enter fair draws, and respond to invitations.',
  },
  organizer: {
    userId: 'jordan',
    description: 'Manage events, run bounded draws, and fill open capacity.',
  },
  admin: {
    userId: 'admin',
    description: 'Review platform activity and moderate users or events.',
  },
};

const navigation = {
  entrant: [
    ['discover', '⌕', 'Discover'],
    ['tickets', '◎', 'My tickets'],
    ['invitations', '✉', 'Invitations'],
    ['activity', '◷', 'Activity'],
  ],
  organizer: [
    ['overview', '▦', 'Overview'],
    ['events', '◆', 'My events'],
    ['participants', '♟', 'Participants'],
    ['activity', '◷', 'Activity'],
  ],
  admin: [
    ['overview', '▦', 'Overview'],
    ['users', '♟', 'Users'],
    ['events', '◆', 'Events'],
    ['activity', '◷', 'Audit log'],
  ],
};

const defaultViews = {
  entrant: 'discover',
  organizer: 'overview',
  admin: 'overview',
};

let state = loadState();
let role = 'entrant';
let view = defaultViews[role];
let search = '';
let category = 'All';
let toastTimer;

const viewRoot = document.querySelector('#app-view');
const roleContext = document.querySelector('#role-context');
const profileChip = document.querySelector('#profile-chip');
const appNavigation = document.querySelector('#app-navigation');
const detailDialog = document.querySelector('#detail-dialog');
const dialogContent = document.querySelector('#dialog-content');
const toast = document.querySelector('#toast');

function loadState() {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY));
    return parsed?.version === 1 ? parsed : seedState();
  } catch {
    return seedState();
  }
}

function persist() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function currentUser() {
  return state.users.find((user) => user.id === roleProfiles[role].userId);
}

function getUser(id) {
  return state.users.find((user) => user.id === id);
}

function getEvent(id) {
  return state.events.find((event) => event.id === id);
}

function selectedStatus(event, userId) {
  return event.selected[userId] || '';
}

function activeSelectedCount(event) {
  return Object.values(event.selected).filter(
    (status) => status === 'pending' || status === 'accepted',
  ).length;
}

function availableCapacity(event) {
  return Math.max(0, event.capacity - activeSelectedCount(event));
}

function addActivity(title, detail) {
  state.audit.unshift({
    id: crypto.randomUUID(),
    title,
    detail,
    time: 'Just now',
  });
  state.audit = state.audit.slice(0, 30);
}

function notify(userId, title, detail) {
  state.notifications.unshift({
    id: crypto.randomUUID(),
    userId,
    title,
    detail,
    time: 'Just now',
  });
}

function flash(message) {
  toast.textContent = message;
  toast.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toast.hidden = true;
  }, 3200);
}

function roleInvitationCount() {
  if (role !== 'entrant') return 0;
  return state.events.filter(
    (event) => selectedStatus(event, currentUser().id) === 'pending',
  ).length;
}

function renderShell() {
  document.querySelectorAll('[data-role]').forEach((button) => {
    const active = button.dataset.role === role;
    button.setAttribute('aria-selected', String(active));
    button.tabIndex = active ? 0 : -1;
  });
  roleContext.textContent = roleProfiles[role].description;
  const user = currentUser();
  profileChip.innerHTML = `
    <span class="avatar">${user.initials}</span>
    <span class="profile-copy">
      <strong>${user.name}</strong>
      <span>${user.role} demo</span>
    </span>
  `;
  appNavigation.innerHTML = navigation[role].map(([id, icon, label]) => {
    const invitationCount = roleInvitationCount();
    const badge = id === 'invitations' && invitationCount
      ? `<span class="nav-badge">${invitationCount}</span>`
      : '';
    return `
      <button
        class="nav-button"
        data-view="${id}"
        type="button"
        ${view === id ? 'aria-current="page"' : ''}
      >
        <span class="nav-icon" aria-hidden="true">${icon}</span>
        <span>${label}</span>
        ${badge}
      </button>
    `;
  }).join('');
}

function pageHeader(eyebrow, title, description, action = '') {
  return `
    <header class="page-header">
      <div>
        <p class="eyebrow">${eyebrow}</p>
        <h1>${title}</h1>
        <p>${description}</p>
      </div>
      ${action ? `<div class="header-action">${action}</div>` : ''}
    </header>
  `;
}

function statusPill(status) {
  const value = status || 'waiting';
  return `<span class="status-pill status-pill--${value}">${value}</span>`;
}

function eventCard(event) {
  const user = currentUser();
  const joined = event.waitlist.includes(user.id);
  const selection = selectedStatus(event, user.id);
  const fill = Math.min(
    100,
    Math.round((activeSelectedCount(event) / event.capacity) * 100),
  );
  let primary = '';

  if (role === 'entrant' && event.active && event.status === 'open') {
    if (selection) {
      primary = `
        <button class="button button--small button--quiet" data-view="invitations" type="button">
          View ${selection}
        </button>
      `;
    } else {
      primary = `
        <button
          class="button button--small ${joined ? 'button--quiet' : 'button--primary'}"
          data-action="${joined ? 'leave' : 'join'}"
          data-event="${event.id}"
          type="button"
        >
          ${joined ? 'Leave waitlist' : 'Join waitlist'}
        </button>
      `;
    }
  }
  if (role === 'organizer') {
    primary = `
      <button
        class="button button--small button--primary"
        data-action="draw"
        data-event="${event.id}"
        type="button"
        ${availableCapacity(event) === 0 || event.waitlist.length === 0 ? 'disabled' : ''}
      >
        Run fair draw
      </button>
    `;
  }
  if (role === 'admin') {
    primary = `
      <button
        class="button button--small ${event.active ? 'button--danger' : 'button--primary'}"
        data-action="toggle-event"
        data-event="${event.id}"
        type="button"
      >
        ${event.active ? 'Deactivate' : 'Restore'}
      </button>
    `;
  }

  return `
    <article class="event-card">
      <div class="event-poster" style="--poster:${event.color}">
        <span class="event-status">${event.active ? event.status : 'inactive'}</span>
        <span aria-hidden="true">${event.icon}</span>
      </div>
      <div class="event-body">
        <div class="event-kicker">
          <span>${event.category}</span>
          <span>${event.date.split(' · ')[0]}</span>
        </div>
        <h2>${event.name}</h2>
        <p>${event.description}</p>
        <div class="capacity-row">
          <span>${activeSelectedCount(event)} / ${event.capacity} selected</span>
          <span class="capacity-track"><span style="width:${fill}%"></span></span>
          <span>${event.waitlist.length} waiting</span>
        </div>
        <div class="card-actions">
          <button class="button button--small button--quiet" data-action="details" data-event="${event.id}" type="button">Details</button>
          ${primary}
        </div>
      </div>
    </article>
  `;
}

function renderEntrant() {
  if (view === 'discover') {
    const categories = ['All', ...new Set(state.events.map((event) => event.category))];
    const events = state.events.filter((event) => {
      const haystack = `${event.name} ${event.category} ${event.location}`.toLowerCase();
      return event.active
        && haystack.includes(search.toLowerCase())
        && (category === 'All' || event.category === category);
    });
    viewRoot.innerHTML = `
      ${pageHeader(
        'Entrant workspace',
        'Find your next event.',
        'Join an open waiting list. Entry order never changes your odds.',
      )}
      <div class="toolbar">
        <label class="search-control">
          <span aria-hidden="true">⌕</span>
          <span class="sr-only">Search events</span>
          <input id="event-search" type="search" value="${escapeHtml(search)}" placeholder="Search events or places" aria-label="Search events">
        </label>
        <div class="filter-chips">
          ${categories.map((item) => `
            <button class="filter-chip" data-category="${item}" aria-pressed="${String(category === item)}" type="button">
              ${item}
            </button>
          `).join('')}
        </div>
      </div>
      ${events.length
        ? `<div class="event-grid">${events.map(eventCard).join('')}</div>`
        : emptyState('⌕', 'No events found', 'Try another search or category.')}
    `;
    return;
  }

  if (view === 'tickets') {
    const userId = currentUser().id;
    const events = state.events.filter(
      (event) => event.waitlist.includes(userId) || selectedStatus(event, userId),
    );
    viewRoot.innerHTML = `
      ${pageHeader(
        'Your activity',
        'My tickets.',
        'Track waiting lists, selections, and confirmed places.',
      )}
      ${events.length
        ? `<div class="event-grid">${events.map(eventCard).join('')}</div>`
        : emptyState('◎', 'Nothing here yet', 'Join a waiting list from Discover to begin.')}
    `;
    return;
  }

  if (view === 'invitations') {
    const userId = currentUser().id;
    const invitations = state.events.filter(
      (event) => selectedStatus(event, userId),
    );
    viewRoot.innerHTML = `
      ${pageHeader(
        'Time-sensitive',
        'Invitations.',
        'Respond to selections and keep your event history current.',
      )}
      <div class="list">
        ${invitations.length
          ? invitations.map((event) => invitationCard(event, userId)).join('')
          : emptyState(
            '✉',
            'No invitations yet',
            'Ask the organizer demo to run a draw, then return here.',
          )}
      </div>
    `;
    return;
  }

  renderActivity(
    'Your notifications',
    state.notifications.filter((item) => item.userId === currentUser().id),
  );
}

function invitationCard(event, userId) {
  const status = selectedStatus(event, userId);
  const actions = status === 'pending'
    ? `
      <button class="button button--small button--primary" data-action="respond" data-response="accepted" data-event="${event.id}" type="button">Accept place</button>
      <button class="button button--small button--danger" data-action="respond" data-response="declined" data-event="${event.id}" type="button">Decline</button>
    `
    : `
      <button class="button button--small button--quiet" data-action="details" data-event="${event.id}" type="button">View details</button>
    `;
  return `
    <article class="invitation">
      <div class="invitation-art" style="background:${event.color}">${event.icon}</div>
      <div class="invitation-body">
        ${statusPill(status)}
        <h2>${event.name}</h2>
        <p>${event.date} · ${event.location}</p>
        <div class="card-actions">${actions}</div>
      </div>
    </article>
  `;
}

function renderOrganizer() {
  const events = state.events.filter(
    (event) => event.organizerId === currentUser().id,
  );
  if (view === 'overview') {
    const totalWaiting = events.reduce(
      (sum, event) => sum + event.waitlist.length,
      0,
    );
    const totalSelected = events.reduce(
      (sum, event) => sum + activeSelectedCount(event),
      0,
    );
    const totalConfirmed = events.reduce(
      (sum, event) => sum + event.confirmed.length,
      0,
    );
    viewRoot.innerHTML = `
      ${pageHeader(
        'Organizer workspace',
        'Run a fair event.',
        'Selections are capacity-aware and every demo action appears in the audit trail.',
        '<button class="button button--primary" data-view="events" type="button">Manage events</button>',
      )}
      <div class="stat-grid">
        ${statCard('Active events', events.filter((event) => event.active).length)}
        ${statCard('Waiting entrants', totalWaiting)}
        ${statCard('Selected', totalSelected)}
        ${statCard('Confirmed', totalConfirmed)}
      </div>
      <div class="content-grid">
        <section class="panel">
          <div class="panel-header"><h2>Event readiness</h2><span>Live demo data</span></div>
          <div class="list">${events.map((event) => eventSummaryRow(event)).join('')}</div>
        </section>
        <section class="panel">
          <div class="panel-header"><h2>Recent activity</h2><span>Newest first</span></div>
          ${activityMarkup(state.audit.slice(0, 5))}
        </section>
      </div>
    `;
    return;
  }

  if (view === 'events') {
    viewRoot.innerHTML = `
      ${pageHeader(
        'Organizer workspace',
        'My events.',
        'Run a draw, inspect selections, and draw replacements when capacity opens.',
      )}
      <div class="event-grid">${events.map(eventCard).join('')}</div>
    `;
    return;
  }

  if (view === 'participants') {
    const rows = events.flatMap((event) => (
      Object.entries(event.selected).map(([userId, status]) => ({
        event,
        user: getUser(userId),
        status,
      }))
    ));
    viewRoot.innerHTML = `
      ${pageHeader(
        'Organizer workspace',
        'Participants.',
        'Selections and entrant responses across your events.',
      )}
      <section class="panel">
        <div class="panel-header"><h2>Selected entrants</h2><span>${rows.length} records</span></div>
        ${rows.length
          ? `<div class="list">${rows.map(participantRow).join('')}</div>`
          : emptyState('♟', 'No selections yet', 'Run a fair draw from My events.')}
      </section>
    `;
    return;
  }

  renderActivity('Organizer activity', state.audit);
}

function participantRow({ event, user, status }) {
  const replacement = status === 'declined' || status === 'cancelled'
    ? `<button class="button button--small button--blue" data-action="replacement" data-event="${event.id}" type="button">Draw replacement</button>`
    : '';
  return `
    <div class="list-row">
      <span class="avatar">${user.initials}</span>
      <span class="list-row__main"><strong>${user.name}</strong><span>${event.name}</span></span>
      ${statusPill(status)}
      ${replacement}
    </div>
  `;
}

function renderAdmin() {
  if (view === 'overview') {
    const activeUsers = state.users.filter((user) => user.active).length;
    const activeEvents = state.events.filter((event) => event.active).length;
    viewRoot.innerHTML = `
      ${pageHeader(
        'Administrator workspace',
        'Platform overview.',
        'Review demo health and moderate records without touching a real backend.',
      )}
      <div class="stat-grid">
        ${statCard('Active users', activeUsers)}
        ${statCard('Active events', activeEvents)}
        ${statCard(
          'Waitlist entries',
          state.events.reduce((sum, event) => sum + event.waitlist.length, 0),
        )}
        ${statCard('Audit records', state.audit.length)}
      </div>
      <div class="content-grid">
        <section class="panel">
          <div class="panel-header"><h2>Moderation queue</h2><span>Demo records</span></div>
          <div class="list">${state.events.map((event) => eventSummaryRow(event, true)).join('')}</div>
        </section>
        <section class="panel">
          <div class="panel-header"><h2>System boundary</h2></div>
          <p class="panel-copy">
            This interactive demo mirrors Ticketjack workflows with browser-local data.
            Firebase Authentication, Firestore, Storage, Cloud Messaging, and callable
            Functions are not contacted.
          </p>
        </section>
      </div>
    `;
    return;
  }

  if (view === 'users') {
    viewRoot.innerHTML = `
      ${pageHeader(
        'Administrator workspace',
        'Users.',
        'Deactivate or restore a seeded demo identity.',
      )}
      <section class="panel">
        <div class="panel-header"><h2>User directory</h2><span>${state.users.length} demo users</span></div>
        <div class="list">${state.users.map(userRow).join('')}</div>
      </section>
    `;
    return;
  }

  if (view === 'events') {
    viewRoot.innerHTML = `
      ${pageHeader(
        'Administrator workspace',
        'Events.',
        'Review and moderate all seeded event records.',
      )}
      <div class="event-grid">${state.events.map(eventCard).join('')}</div>
    `;
    return;
  }

  renderActivity('Platform audit log', state.audit);
}

function userRow(user) {
  return `
    <div class="list-row">
      <span class="avatar">${user.initials}</span>
      <span class="list-row__main"><strong>${user.name}</strong><span>${user.role}</span></span>
      ${statusPill(user.active ? 'active' : 'inactive')}
      <button
        class="button button--small ${user.active ? 'button--danger' : 'button--primary'}"
        data-action="toggle-user"
        data-user="${user.id}"
        type="button"
        ${user.id === 'admin' ? 'disabled title="The current demo administrator stays active"' : ''}
      >
        ${user.active ? 'Deactivate' : 'Restore'}
      </button>
    </div>
  `;
}

function renderActivity(title, items) {
  viewRoot.innerHTML = `
    ${pageHeader(
      'Activity',
      `${title}.`,
      'A transparent record of actions taken in this browser session.',
    )}
    <section class="panel">
      <div class="panel-header"><h2>Timeline</h2><span>${items.length} records</span></div>
      ${items.length
        ? activityMarkup(items)
        : emptyState('◷', 'No activity yet', 'Complete a workflow to add an entry.')}
    </section>
  `;
}

function activityMarkup(items) {
  return `
    <div>
      ${items.map((item) => `
        <div class="activity-item">
          <strong>${item.title}</strong>
          <span>${item.detail} · ${item.time}</span>
        </div>
      `).join('')}
    </div>
  `;
}

function statCard(label, value) {
  return `<article class="stat-card"><span>${label}</span><strong>${value}</strong></article>`;
}

function eventSummaryRow(event, moderation = false) {
  const moderationAction = moderation
    ? `
      <button
        class="button button--small ${event.active ? 'button--danger' : 'button--primary'}"
        data-action="toggle-event"
        data-event="${event.id}"
        type="button"
      >
        ${event.active ? 'Deactivate' : 'Restore'}
      </button>
    `
    : '';
  return `
    <div class="list-row">
      <span class="avatar" style="background:${event.color}">${event.icon}</span>
      <span class="list-row__main">
        <strong>${event.name}</strong>
        <span>${event.waitlist.length} waiting · ${activeSelectedCount(event)}/${event.capacity} selected</span>
      </span>
      ${statusPill(event.active ? event.status : 'inactive')}
      <span class="row-actions">
        <button class="button button--small button--quiet" data-action="details" data-event="${event.id}" type="button">Inspect</button>
        ${moderationAction}
      </span>
    </div>
  `;
}

function emptyState(icon, title, description) {
  return `
    <div class="empty-state">
      <div>
        <b aria-hidden="true">${icon}</b>
        <h2>${title}</h2>
        <p>${description}</p>
      </div>
    </div>
  `;
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (character) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;',
  }[character]));
}

function render() {
  renderShell();
  if (role === 'entrant') renderEntrant();
  if (role === 'organizer') renderOrganizer();
  if (role === 'admin') renderAdmin();
}

function showDetails(eventId) {
  const event = getEvent(eventId);
  const selected = Object.entries(event.selected);
  dialogContent.innerHTML = `
    <div class="dialog-hero" style="--poster:${event.color}">
      <span aria-hidden="true">${event.icon}</span>
      <button class="dialog-close" data-action="close-dialog" type="button" aria-label="Close details">×</button>
    </div>
    <div class="dialog-body">
      <p class="eyebrow">${event.category} · ${event.active ? event.status : 'inactive'}</p>
      <h2>${event.name}</h2>
      <p>${event.description}</p>
      <div class="detail-facts">
        <div><span>When</span><strong>${event.date}</strong></div>
        <div><span>Where</span><strong>${event.location}</strong></div>
        <div><span>Capacity</span><strong>${event.capacity} places</strong></div>
      </div>
      <section class="panel">
        <div class="panel-header"><h2>Draw status</h2><span>${event.waitlist.length} waiting</span></div>
        ${selected.length
          ? `<div class="list">${selected.map(([userId, status]) => selectedRow(userId, status)).join('')}</div>`
          : '<p class="panel-copy">No entrants have been selected yet.</p>'}
      </section>
      <div class="dialog-actions">
        ${role === 'organizer'
          ? `
            <button
              class="button button--primary"
              data-action="draw"
              data-event="${event.id}"
              type="button"
              ${availableCapacity(event) === 0 || event.waitlist.length === 0 ? 'disabled' : ''}
            >
              Run fair draw
            </button>
          `
          : ''}
        <button class="button button--quiet" data-action="close-dialog" type="button">Close</button>
      </div>
    </div>
  `;
  if (!detailDialog.open) detailDialog.showModal();
}

function selectedRow(userId, status) {
  const user = getUser(userId);
  return `
    <div class="list-row">
      <span class="avatar">${user.initials}</span>
      <span class="list-row__main"><strong>${user.name}</strong><span>Selected entrant</span></span>
      ${statusPill(status)}
    </div>
  `;
}

function joinEvent(eventId) {
  const event = getEvent(eventId);
  const user = currentUser();
  if (!event.active || event.status !== 'open' || event.waitlist.includes(user.id)) return;
  event.waitlist.push(user.id);
  notify(user.id, 'Waiting list joined', `${event.name} will notify you after the draw.`);
  addActivity('Entrant joined waiting list', `${user.name} joined ${event.name}.`);
  persist();
  flash(`You joined ${event.name}.`);
}

function leaveEvent(eventId) {
  const event = getEvent(eventId);
  const user = currentUser();
  event.waitlist = event.waitlist.filter((id) => id !== user.id);
  addActivity('Entrant left waiting list', `${user.name} left ${event.name}.`);
  persist();
  flash(`You left ${event.name}.`);
}

function runDraw(eventId, replacement = false) {
  const event = getEvent(eventId);
  const slots = replacement ? 1 : availableCapacity(event);
  const eligible = event.waitlist.filter(
    (userId) => !event.selected[userId] && getUser(userId)?.active,
  );
  if (!event.active || slots <= 0 || eligible.length === 0) {
    flash('No eligible entrants or open capacity remain.');
    return;
  }

  const picked = [];
  const drawCount = Math.min(slots, eligible.length);
  for (let index = 0; index < drawCount; index += 1) {
    const pickIndex = (state.drawCounter + index * 2) % eligible.length;
    const [userId] = eligible.splice(pickIndex, 1);
    picked.push(userId);
  }
  state.drawCounter += 1;
  picked.forEach((userId) => {
    event.selected[userId] = 'pending';
    notify(userId, 'You were selected', `Respond to your invitation for ${event.name}.`);
  });
  addActivity(
    replacement ? 'Replacement drawn' : 'Fair draw completed',
    `${picked.length} entrant${picked.length === 1 ? '' : 's'} selected for ${event.name}.`,
  );
  persist();
  flash(`${picked.length} entrant${picked.length === 1 ? '' : 's'} selected.`);
  if (detailDialog.open) showDetails(eventId);
}

function respondToInvitation(eventId, response) {
  const event = getEvent(eventId);
  const user = currentUser();
  if (event.selected[user.id] !== 'pending') return;
  event.selected[user.id] = response;
  if (response === 'accepted' && !event.confirmed.includes(user.id)) {
    event.confirmed.push(user.id);
  }
  if (response === 'declined') {
    event.confirmed = event.confirmed.filter((id) => id !== user.id);
  }
  addActivity(
    `Invitation ${response}`,
    `${user.name} ${response} ${event.name}.`,
  );
  notify(
    user.id,
    `Invitation ${response}`,
    response === 'accepted'
      ? 'Your place is confirmed.'
      : 'The organizer can now draw a replacement.',
  );
  persist();
  flash(
    response === 'accepted'
      ? 'Your place is confirmed.'
      : 'Invitation declined.',
  );
}

function toggleUser(userId) {
  const user = getUser(userId);
  if (!user || user.id === 'admin') return;
  user.active = !user.active;
  addActivity(
    `User ${user.active ? 'restored' : 'deactivated'}`,
    `${user.name} is now ${user.active ? 'active' : 'inactive'}.`,
  );
  persist();
  flash(`${user.name} ${user.active ? 'restored' : 'deactivated'}.`);
}

function toggleEvent(eventId) {
  const event = getEvent(eventId);
  event.active = !event.active;
  addActivity(
    `Event ${event.active ? 'restored' : 'deactivated'}`,
    `${event.name} is now ${event.active ? 'active' : 'inactive'}.`,
  );
  persist();
  flash(`${event.name} ${event.active ? 'restored' : 'deactivated'}.`);
}

document.addEventListener('click', (event) => {
  const roleButton = event.target.closest('[data-role]');
  if (roleButton) {
    role = roleButton.dataset.role;
    view = defaultViews[role];
    search = '';
    category = 'All';
    render();
    document.querySelector('#demo-main').focus();
    return;
  }

  const viewButton = event.target.closest('[data-view]');
  if (viewButton) {
    view = viewButton.dataset.view;
    render();
    document.querySelector('#demo-main').focus();
    return;
  }

  const categoryButton = event.target.closest('[data-category]');
  if (categoryButton) {
    category = categoryButton.dataset.category;
    render();
    return;
  }

  const actionButton = event.target.closest('[data-action]');
  if (!actionButton) return;
  const {
    action,
    event: eventId,
    response,
    user,
  } = actionButton.dataset;

  if (action === 'details') showDetails(eventId);
  if (action === 'close-dialog') detailDialog.close();
  if (action === 'join') joinEvent(eventId);
  if (action === 'leave') leaveEvent(eventId);
  if (action === 'draw') runDraw(eventId);
  if (action === 'replacement') runDraw(eventId, true);
  if (action === 'respond') respondToInvitation(eventId, response);
  if (action === 'toggle-user') toggleUser(user);
  if (action === 'toggle-event') toggleEvent(eventId);
  render();
});

document.addEventListener('input', (event) => {
  if (event.target.id !== 'event-search') return;
  search = event.target.value;
  renderEntrant();
  const input = document.querySelector('#event-search');
  input.focus();
  input.setSelectionRange(search.length, search.length);
});

document.querySelector('#reset-demo').addEventListener('click', () => {
  state = seedState();
  persist();
  role = 'entrant';
  view = defaultViews.entrant;
  search = '';
  category = 'All';
  if (detailDialog.open) detailDialog.close();
  render();
  flash('Demo data reset.');
});

detailDialog.addEventListener('click', (event) => {
  if (event.target === detailDialog) detailDialog.close();
});

render();
