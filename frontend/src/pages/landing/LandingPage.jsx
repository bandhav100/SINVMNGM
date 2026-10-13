import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Building2,
  Boxes,
  TrendingUp,
  Coins,
  ArrowRight,
  LogIn,
  PlusCircle,
  Menu,
  X as CloseIcon,
  Layers,
  FolderKanban,
  ShieldCheck,
  Sparkles,
  Mail,
  Phone
} from 'lucide-react';
import heroBg1 from '../../assets/hero-bg-1.jpg';
import heroBg2 from '../../assets/hero-bg-2.jpg';
import heroBg3 from '../../assets/hero-bg-3.jpg';
import heroBg4 from '../../assets/hero-bg-4.jpg';
import heroBg5 from '../../assets/hero-bg-5.jpg';
import heroBg6 from '../../assets/hero-bg-6.jpg';
import '../../styles/landing.css';

const heroSlides = [
  { img: heroBg1, label: 'Central Distribution Warehouse' },
  { img: heroBg2, label: 'Modern Architectural Storage Facility' },
  { img: heroBg3, label: 'Organized Parcel Logistics Depot' },
  { img: heroBg4, label: 'High-Bay Skylight Inventory Center' },
  { img: heroBg5, label: 'Automated Logistics Terminal' },
  { img: heroBg6, label: 'Grand Multi-Tier Pallet Depot' }
];

export default function LandingPage() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [currentSlide, setCurrentSlide] = useState(0);
  const navigate = useNavigate();

  // Automatic background slideshow every 2.6 seconds (within the requested 2-3s range)
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % heroSlides.length);
    }, 2600);

    return () => clearInterval(timer);
  }, []);

  const scrollToSection = (id) => {
    setMobileMenuOpen(false);
    const element = document.getElementById(id);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <div className="landing-container">
      {/* ===================================================================
          Hero & Navigation Wrapper with Automatic Background Slideshow
          =================================================================== */}
      <section className="landing-hero-wrapper" id="home">
        {/* Automatic Multi-Image Background Slideshow */}
        <div className="landing-hero-slideshow" aria-hidden="true">
          {heroSlides.map((slide, idx) => (
            <div
              key={idx}
              className={`landing-hero-slide ${idx === currentSlide ? 'active' : ''}`}
              style={{ backgroundImage: `url(${slide.img})` }}
            />
          ))}
        </div>

        {/* Cinematic dark overlay for text clarity */}
        <div className="landing-hero-overlay" />

        {/* Top Navigation Bar */}
        <div className="landing-nav-container">
          <nav className="landing-nav">
            {/* Logo Emblem */}
            <Link to="/" className="landing-brand">
              <div className="landing-logo-emblem">
                <Building2 size={24} />
              </div>
              <div className="landing-brand-text">
                <span className="landing-brand-name">STOCKWISE</span>
                <span className="landing-brand-subtitle">SYSTEMS &bull; SIMS</span>
              </div>
            </Link>

            {/* Desktop Nav Links */}
            <div className="landing-nav-links">
              <button
                type="button"
                className="landing-nav-link active"
                onClick={() => scrollToSection('home')}
              >
                Home
              </button>
              <button
                type="button"
                className="landing-nav-link"
                onClick={() => scrollToSection('about')}
              >
                About
              </button>
              <button
                type="button"
                className="landing-nav-link"
                onClick={() => scrollToSection('capabilities')}
              >
                Capabilities
              </button>
              <button
                type="button"
                className="landing-nav-link"
                onClick={() => scrollToSection('contact')}
              >
                Contact Us
              </button>
            </div>

            {/* Top Right Action Buttons */}
            <div className="landing-nav-actions">
              <Link to="/login" className="btn-nav-signin">
                Sign In
              </Link>
              <Link to="/create-inventory" className="btn-nav-create">
                Create Inventory
              </Link>
              <button
                type="button"
                className="landing-mobile-toggle"
                onClick={() => setMobileMenuOpen(true)}
                aria-label="Open navigation menu"
              >
                <Menu size={22} />
              </button>
            </div>
          </nav>

          {/* Thin Horizontal Divider Line */}
          <div className="landing-divider-line" />
        </div>

        {/* Hero Main Content */}
        <div className="landing-hero-content">
          <div className="hero-pill-badge">
            <Sparkles size={14} className="hero-pill-icon" />
            <span>NEXT-GEN ENTERPRISE INVENTORY PLATFORM</span>
          </div>

          <h1 className="hero-headline">
            Experience the Power of<br />Informed Inventory
          </h1>

          <p className="hero-subhead">
            Build a Strong Foundation for Your Inventory, Operations, and Real-Time Forecasting Today.
          </p>

          <div className="hero-cta-group">
            <Link to="/create-inventory" className="btn-hero-primary">
              <span>Create Inventory</span>
              <ArrowRight size={18} />
            </Link>

            <Link to="/login" className="btn-hero-secondary">
              <LogIn size={18} />
              <span>Sign In to Workspace</span>
            </Link>
          </div>

          {/* Slide Indicator Dots */}
          <div className="hero-slideshow-dots" aria-label="Slideshow indicators">
            {heroSlides.map((_, idx) => (
              <button
                key={idx}
                type="button"
                className={`hero-dot ${idx === currentSlide ? 'active' : ''}`}
                onClick={() => setCurrentSlide(idx)}
                aria-label={`Jump to slide ${idx + 1}`}
              />
            ))}
          </div>
        </div>
      </section>

      {/* ===================================================================
          Section 2: About / "What We Do"
          =================================================================== */}
      <section className="landing-about-section" id="about">
        <div className="landing-about-container">
          <div className="about-left-col">
            <span className="section-eyebrow">WHAT WE DO</span>
            <h2 className="about-headline">
              Simplifying Inventory Management for Modern Businesses
            </h2>
          </div>

          <div className="about-right-col">
            <p className="about-description">
              Stockwise Systems provides real-time visibility, automated low-stock warnings, purchase ordering, and sales order fulfillment so businesses can operate with speed, agility, and precision.
            </p>
            <div className="about-stats-row">
              <div className="about-stat-item">
                <span className="stat-number">99.9%</span>
                <span className="stat-label">Inventory Accuracy</span>
              </div>
              <div className="about-stat-item">
                <span className="stat-number">Real-Time</span>
                <span className="stat-label">Pessimistic Locking</span>
              </div>
              <div className="about-stat-item">
                <span className="stat-number">Multi-Tenant</span>
                <span className="stat-label">Isolated Workspaces</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ===================================================================
          Section 3: Capabilities & Features
          =================================================================== */}
      <section className="landing-services-section" id="capabilities">
        <div className="landing-services-container">
          <div className="services-header">
            <span className="section-eyebrow">OUR CAPABILITIES</span>
            <h2 className="services-title">Services Tailored for Scalable Commerce</h2>
            <p className="services-subtitle">
              Enterprise-grade modules designed to keep your supply chain synchronized from warehouse floor to executive boardroom.
            </p>
          </div>

          <div className="services-grid">
            <div className="service-card">
              <div className="service-icon-wrap">
                <Boxes size={24} />
              </div>
              <h3 className="service-card-title">Real-Time Stock Tracking</h3>
              <p className="service-card-desc">
                Pessimistic locking and audit-backed movements ensure quantity integrity with zero over-allocation risk.
              </p>
            </div>

            <div className="service-card">
              <div className="service-icon-wrap">
                <FolderKanban size={24} />
              </div>
              <h3 className="service-card-title">Purchase &amp; Supplier Flow</h3>
              <p className="service-card-desc">
                Structured purchase orders, auto-receiving workflows, and active supplier catalogs simplify restocking.
              </p>
            </div>

            <div className="service-card">
              <div className="service-icon-wrap">
                <TrendingUp size={24} />
              </div>
              <h3 className="service-card-title">Sales Fulfillment</h3>
              <p className="service-card-desc">
                Seamless draft-to-completed sales order pipelines with instant stock adjustments and customer tracking.
              </p>
            </div>

            <div className="service-card">
              <div className="service-icon-wrap">
                <Coins size={24} />
              </div>
              <h3 className="service-card-title">Financial Insights</h3>
              <p className="service-card-desc">
                Visual stock valuation, fast/slow moving categorization, and interactive period comparisons.
              </p>
            </div>

            <div className="service-card">
              <div className="service-icon-wrap">
                <ShieldCheck size={24} />
              </div>
              <h3 className="service-card-title">Role-Based Security</h3>
              <p className="service-card-desc">
                Granular authorization for Admin, Inventory Manager, Purchase Manager, and Sales Staff with HttpOnly sessions.
              </p>
            </div>

            <div className="service-card">
              <div className="service-icon-wrap">
                <Layers size={24} />
              </div>
              <h3 className="service-card-title">Multi-Tenant Workspaces</h3>
              <p className="service-card-desc">
                Independent companies manage isolated catalogs, orders, and metrics in a unified high-performance platform.
              </p>
            </div>
          </div>

          {/* Bottom Action Bar */}
          <div className="landing-bottom-bar">
            <Link to="/login" className="btn-bottom-outline">
              <LogIn size={16} />
              <span>Sign In to Existing Workspace</span>
            </Link>

            <Link to="/create-inventory" className="btn-bottom-solid">
              <PlusCircle size={16} />
              <span>Create New Inventory Workspace</span>
            </Link>
          </div>
        </div>
      </section>

      {/* ===================================================================
          Section 4: Direct Contact & Social Links in Line
          =================================================================== */}
      <section className="landing-contact-section" id="contact">
        <div className="landing-contact-container">
          <div className="landing-contact-header">
            <span className="contact-pill">DIRECT SUPPORT &amp; INQUIRIES</span>
            <h2 className="contact-title">Connect with Our Service</h2>
            <p className="contact-desc">
              Have questions regarding multi-company registration, enterprise onboarding, or platform support? Reach out directly through any of our verified channels below.
            </p>
          </div>

          {/* In-Line Contact Info & Social Logos */}
          <div className="landing-contact-inline-bar">
            {/* Email item */}
            <a href="mailto:smartinventory@service.org" className="contact-inline-item">
              <div className="contact-icon-bubble email-bubble">
                <Mail size={18} />
              </div>
              <div className="contact-item-text">
                <span className="contact-label">EMAIL ID</span>
                <span className="contact-value">smartinventory@service.org</span>
              </div>
            </a>

            <div className="contact-divider" />

            {/* Phone item */}
            <a href="tel:040234654" className="contact-inline-item">
              <div className="contact-icon-bubble phone-bubble">
                <Phone size={18} />
              </div>
              <div className="contact-item-text">
                <span className="contact-label">CONTACT PHONE</span>
                <span className="contact-value">040234654</span>
              </div>
            </a>

            <div className="contact-divider" />

            {/* X (formerly Twitter) Logo */}
            <a
              href="https://x.com"
              target="_blank"
              rel="noopener noreferrer"
              className="contact-inline-item social-link"
              title="Official X Profile"
              aria-label="X Twitter Logo"
            >
              <div className="contact-icon-bubble x-bubble">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
                </svg>
              </div>
              <div className="contact-item-text">
                <span className="contact-label">CONNECT ON</span>
                <span className="contact-value">X (Twitter)</span>
              </div>
            </a>

            <div className="contact-divider" />

            {/* WhatsApp Logo */}
            <a
              href="https://wa.me/91040234654"
              target="_blank"
              rel="noopener noreferrer"
              className="contact-inline-item social-link"
              title="Chat with us on WhatsApp"
              aria-label="WhatsApp Logo"
            >
              <div className="contact-icon-bubble whatsapp-bubble">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51l-.57-.01c-.198 0-.52.074-.792.372s-1.04 1.016-1.04 2.479 1.065 2.876 1.213 3.074c.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.885-9.885 9.885m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L0 24l6.335-1.662c1.746.953 3.71 1.455 5.71 1.456h.005c6.554 0 11.89-5.336 11.893-11.893a11.82 11.82 0 00-3.48-8.414z" />
                </svg>
              </div>
              <div className="contact-item-text">
                <span className="contact-label">WHATSAPP CHAT</span>
                <span className="contact-value">Instant Connect</span>
              </div>
            </a>
          </div>
        </div>
      </section>

      {/* ===================================================================
          Footer
          =================================================================== */}
      <footer className="landing-footer">
        <div className="landing-footer-container">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <Building2 size={18} style={{ color: '#818cf8' }} />
            <span>&copy; {new Date().getFullYear()} Stockwise Systems &bull; Smart Inventory Management System.</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', color: '#64748b', fontSize: '0.85rem' }}>
            <span>Email: <strong style={{ color: '#94a3b8' }}>smartinventory@service.org</strong></span>
            <span>Tel: <strong style={{ color: '#94a3b8' }}>040234654</strong></span>
            <span>Enterprise Edition v1.0.0</span>
          </div>
        </div>
      </footer>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div className="landing-mobile-drawer">
          <button
            type="button"
            className="landing-mobile-close"
            onClick={() => setMobileMenuOpen(false)}
            aria-label="Close menu"
          >
            <CloseIcon size={24} />
          </button>

          <Link
            to="/"
            className="landing-brand"
            style={{ marginBottom: '1rem' }}
            onClick={() => setMobileMenuOpen(false)}
          >
            <div className="landing-logo-emblem">
              <Building2 size={24} />
            </div>
            <div className="landing-brand-text">
              <span className="landing-brand-name">STOCKWISE</span>
              <span className="landing-brand-subtitle">SYSTEMS</span>
            </div>
          </Link>

          <button
            type="button"
            className="landing-nav-link"
            style={{ textAlign: 'left', fontSize: '1.2rem' }}
            onClick={() => scrollToSection('home')}
          >
            Home
          </button>
          <button
            type="button"
            className="landing-nav-link"
            style={{ textAlign: 'left', fontSize: '1.2rem' }}
            onClick={() => scrollToSection('about')}
          >
            About
          </button>
          <button
            type="button"
            className="landing-nav-link"
            style={{ textAlign: 'left', fontSize: '1.2rem' }}
            onClick={() => scrollToSection('capabilities')}
          >
            Capabilities
          </button>
          <button
            type="button"
            className="landing-nav-link"
            style={{ textAlign: 'left', fontSize: '1.2rem' }}
            onClick={() => scrollToSection('contact')}
          >
            Contact Us
          </button>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '2rem' }}>
            <Link
              to="/login"
              className="btn-hero-secondary"
              onClick={() => setMobileMenuOpen(false)}
            >
              Sign In
            </Link>
            <Link
              to="/create-inventory"
              className="btn-hero-primary"
              onClick={() => setMobileMenuOpen(false)}
            >
              Create Inventory
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
