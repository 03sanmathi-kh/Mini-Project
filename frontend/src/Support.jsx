import React, { useState } from 'react';
import { api, message } from './api';
import { Alert, StatusBadge, formatDate, pretty } from './ui';

export const supportCategories = ['FORGOT_PASSWORD', 'FORGOT_PERMITWORKS_EMAIL', 'ACCOUNT_LOCKED', 'ACCOUNT_DISABLED', 'EMAIL_NOT_RECOGNIZED', 'NEED_NEW_ACCOUNT', 'LOGIN_PROBLEM', 'OTHER'];
const empty = { fullName: '', personalContactEmail: '', permitworksEmail: '', contactPhone: '', category: '', subject: '', description: '' };

export default function SupportModal({ onClose }) {
 const [mode, setMode] = useState('request');
 const [form, setForm] = useState(empty);
 const [check, setCheck] = useState({ ticketNumber: '', personalContactEmail: '' });
 const [errors, setErrors] = useState({});
 const [busy, setBusy] = useState(false);
 const [error, setError] = useState('');
 const [created, setCreated] = useState(null);
 const [ticket, setTicket] = useState(null);
 const change = (setter, state) => event => setter({ ...state, [event.target.name]: event.target.value });

 function validate() {
  const next = {};
  if (!form.fullName.trim()) next.fullName = 'Enter your full name.';
  if (!/^\S+@\S+\.\S+$/.test(form.personalContactEmail.trim())) next.personalContactEmail = 'Enter a valid personal contact email.';
  if (form.permitworksEmail && !/^[a-z0-9][a-z0-9._-]{0,63}@permitworks\.com$/i.test(form.permitworksEmail.trim())) next.permitworksEmail = 'Enter a valid @permitworks.com email or leave this blank.';
  if (form.contactPhone && !/^[+0-9][0-9 ()-]{6,29}$/.test(form.contactPhone.trim())) next.contactPhone = 'Enter a valid phone number.';
  if (!form.category) next.category = 'Select an issue category.';
  if (!form.subject.trim()) next.subject = 'Enter a subject.';
  if (!form.description.trim()) next.description = 'Describe the sign-in or account issue.';
  setErrors(next);
  return !Object.keys(next).length;
 }

 async function submit(event) {
  event.preventDefault();
  if (!validate()) return;
  setBusy(true); setError('');
  try { setCreated((await api.post('/support', form)).data); }
  catch (requestError) { setError(message(requestError)); }
  finally { setBusy(false); }
 }

 async function lookup(event) {
  event.preventDefault(); setBusy(true); setError(''); setTicket(null);
  try { setTicket((await api.post('/support/status', check)).data); }
  catch (requestError) { setError(requestError.response?.status === 404 ? 'The ticket details could not be found. Check the ticket number and personal email.' : message(requestError)); }
  finally { setBusy(false); }
 }

 function switchMode(next) { setMode(next); setError(''); }

 return <div className="overlay support-overlay" onMouseDown={event => event.target === event.currentTarget && !busy && onClose()}>
  <section className="modal support-modal" role="dialog" aria-modal="true" aria-labelledby="support-title">
   <div className="modal-heading"><div><span className="eyebrow">PermitWorks assistance</span><h2 id="support-title">Help and support</h2></div><button className="btn btn-icon" aria-label="Close support" onClick={onClose}>&times;</button></div>
   <div className="support-tabs" role="tablist"><button className={mode === 'request' ? 'active' : ''} onClick={() => switchMode('request')}>Request help</button><button className={mode === 'status' ? 'active' : ''} onClick={() => switchMode('status')}>Check ticket status</button></div>
   <Alert>{error}</Alert>
   {mode === 'request' ? (created ? <div className="support-success"><h3>Your support request has been submitted successfully.</h3><p>Save your ticket number.</p><strong>{created.ticketNumber}</strong><button className="btn btn-primary" onClick={onClose}>Done</button></div> :
    <form onSubmit={submit}>
     <p className="support-guidance">Enter your personal email so the PermitWorks administrator can contact you. Your PermitWorks email is optional if you do not know it.</p>
     <div className="form-grid">
      <SupportField label="Full name" name="fullName" value={form.fullName} onChange={change(setForm, form)} error={errors.fullName} maxLength="100" required />
      <SupportField label="Personal contact email" name="personalContactEmail" type="email" value={form.personalContactEmail} onChange={change(setForm, form)} error={errors.personalContactEmail} maxLength="254" required />
      <SupportField label="PermitWorks login email" name="permitworksEmail" type="email" value={form.permitworksEmail} onChange={change(setForm, form)} error={errors.permitworksEmail} placeholder="Optional" maxLength="254" />
      <SupportField label="Contact phone number" name="contactPhone" type="tel" value={form.contactPhone} onChange={change(setForm, form)} error={errors.contactPhone} placeholder="Optional" maxLength="30" />
      <label className="field"><span>Issue category <b>*</b></span><select name="category" value={form.category} onChange={change(setForm, form)} aria-invalid={!!errors.category}><option value="">Select a category</option>{supportCategories.map(category => <option key={category} value={category}>{pretty(category)}</option>)}</select>{errors.category && <small className="field-error">{errors.category}</small>}</label>
      <SupportField label="Subject" name="subject" value={form.subject} onChange={change(setForm, form)} error={errors.subject} maxLength="160" required />
      <SupportField label="Description" name="description" value={form.description} onChange={change(setForm, form)} error={errors.description} maxLength="4000" area wide required />
     </div>
     <div className="modal-actions"><button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button><button className="btn btn-primary" disabled={busy}>{busy ? 'Submitting...' : 'Submit request'}</button></div>
    </form>) :
    <form onSubmit={lookup}>
     <p className="support-guidance">Use the personal email supplied with your request. Your PermitWorks login email is not required.</p>
     <SupportField label="Ticket number" name="ticketNumber" value={check.ticketNumber} onChange={change(setCheck, check)} placeholder="PW-HELP-2026-00001" maxLength="30" required />
     <SupportField label="Personal contact email" name="personalContactEmail" type="email" value={check.personalContactEmail} onChange={change(setCheck, check)} maxLength="254" required />
     <button className="btn btn-primary support-check" disabled={busy}>{busy ? 'Checking...' : 'Check status'}</button>
     {ticket && <div className="ticket-result"><div><small>Ticket number</small><strong>{ticket.ticketNumber}</strong></div><div><small>Status</small><StatusBadge value={ticket.status} /></div><div><small>Category</small><strong>{pretty(ticket.category)}</strong></div><div><small>Submitted</small><strong>{formatDate(ticket.submittedAt)}</strong></div><div><small>Last updated</small><strong>{formatDate(ticket.updatedAt)}</strong></div>{ticket.userVisibleResponse && <div className="ticket-response"><small>Administrator response</small><p>{ticket.userVisibleResponse}</p></div>}</div>}
    </form>}
  </section>
 </div>;
}

function SupportField({ label, error, area, wide, ...props }) {
 return <label className={`field ${wide ? 'field-wide' : ''}`}><span>{label} {props.required && <b>*</b>}</span>{area ? <textarea {...props} aria-invalid={!!error} /> : <input {...props} aria-invalid={!!error} />}{error && <small className="field-error">{error}</small>}</label>;
}
