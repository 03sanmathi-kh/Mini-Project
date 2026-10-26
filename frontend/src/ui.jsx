import React from 'react';

export const roleLabels={ADMIN:'Administrator',APPLICANT:'Applicant',DEPARTMENT_OFFICER:'Department Officer',PERMIT_REVIEWER:'Permit Reviewer',ZONING_OFFICER:'Zoning Officer',SENIOR_DEPARTMENT_OFFICER:'Senior Department Officer',FINANCE_OFFICER:'Finance Officer',BUILDING_INSPECTOR:'Building Inspector'};

export function BrandMark(){return <span className="brand-mark" aria-hidden="true"><span>P</span></span>}

export function Icon({name}){
 const paths={dashboard:<><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></>,create:<><path d="M12 5v14M5 12h14"/></>,users:<><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/></>,activity:<><path d="M3 12h4l2-7 4 14 2-7h6"/></>,logout:<><path d="M10 17l5-5-5-5M15 12H3M21 19V5a2 2 0 0 0-2-2h-6"/></>,arrow:<><path d="M5 12h14M13 6l6 6-6 6"/></>,back:<><path d="M19 12H5M11 18l-6-6 6-6"/></>,search:<><circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/></>,calendar:<><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/></>,file:<><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M8 13h8M8 17h6"/></>};
 return <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>
}

export function StatusBadge({value}){return <span className={`status status-${String(value||'inactive').toLowerCase()}`}>{pretty(value)}</span>}
export function pretty(value){return String(value||'').replaceAll('_',' ').toLowerCase().replace(/\b\w/g,x=>x.toUpperCase())}
export function formatDate(value,{dateOnly=false}={}){if(!value)return 'Not available';const d=new Date(value);return dateOnly?d.toLocaleDateString(undefined,{day:'numeric',month:'short',year:'numeric'}):d.toLocaleString(undefined,{day:'numeric',month:'short',year:'numeric',hour:'numeric',minute:'2-digit'})}
export function formatMoney(value){return value==null?'Not available':new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR',maximumFractionDigits:2}).format(Number(value))}
export function initials(name=''){return name.split(/\s+/).filter(Boolean).slice(0,2).map(x=>x[0]).join('').toUpperCase()||'PW'}

export function Alert({type='error',children}){return children?<div className={`notice notice-${type}`} role={type==='error'?'alert':'status'}>{children}</div>:null}
export function EmptyState({title='Not available yet',text}){return <div className="empty-state"><strong>{title}</strong>{text&&<p>{text}</p>}</div>}
export function LoadingRows({columns=7,label='Loading applications…'}){return <tr><td className="table-state" colSpan={columns}><span className="spinner"/> {label}</td></tr>}
export function Panel({title,subtitle,children,className=''}){return <section className={`panel ${className}`}><div className="panel-heading"><div><h2>{title}</h2>{subtitle&&<p>{subtitle}</p>}</div></div>{children}</section>}
export function DataItem({label,value,wide=false}){return <div className={`data-item ${wide?'data-wide':''}`}><dt>{label}</dt><dd>{value||'Not available'}</dd></div>}
