import { useCallback, useEffect } from "react";
import { motion, useReducedMotion } from "framer-motion";
import { BarChart3, Contact, Database, ShieldCheck, UsersRound } from "lucide-react";
import tadamunLogo from "../assets/tadamun-logo.svg";

type PreLoginIntroProps = {
  onDone: () => void;
};

const floatingCards = [
  { label: "Customers", icon: UsersRound, className: "left-[8%] top-[26%]" },
  { label: "Leads", icon: BarChart3, className: "right-[10%] top-[22%]" },
  { label: "Contacts", icon: Contact, className: "left-[13%] bottom-[24%]" },
  { label: "Secure API", icon: ShieldCheck, className: "right-[14%] bottom-[27%]" },
];

export function PreLoginIntro({ onDone }: PreLoginIntroProps) {
  const reducedMotion = useReducedMotion();

  const finish = useCallback(() => {
    sessionStorage.setItem("crm-intro-seen", "true");
    onDone();
  }, [onDone]);

  useEffect(() => {
    const timeout = window.setTimeout(finish, reducedMotion ? 450 : 3200);
    return () => window.clearTimeout(timeout);
  }, [finish, reducedMotion]);

  return (
    <main className="relative grid min-h-screen place-items-center overflow-hidden bg-[#020202] text-white">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,rgba(65,192,242,0.22),transparent_26rem),radial-gradient(circle_at_70%_30%,rgba(178,138,46,0.18),transparent_20rem)]" />
      <div className="absolute inset-0 bg-[linear-gradient(to_right,rgba(173,223,241,0.06)_1px,transparent_1px),linear-gradient(to_bottom,rgba(173,223,241,0.06)_1px,transparent_1px)] bg-[size:58px_58px]" />

      <button
        type="button"
        onClick={finish}
        className="absolute right-5 top-5 z-20 rounded-xl border border-white/10 bg-white/10 px-4 py-2 text-sm font-semibold text-white/80 backdrop-blur transition hover:bg-white/15 hover:text-white"
      >
        Skip
      </button>

      <section className="relative z-10 flex w-full max-w-5xl flex-col items-center px-6 text-center">
        {!reducedMotion &&
          floatingCards.map((card, index) => {
            const Icon = card.icon;

            return (
              <motion.div
                key={card.label}
                className={`absolute hidden rounded-2xl border border-white/10 bg-white/[0.07] px-4 py-3 text-left shadow-2xl backdrop-blur-xl md:block ${card.className}`}
                initial={{ opacity: 0, y: 20, rotateX: 18 }}
                animate={{ opacity: 1, y: [0, -10, 0], rotateX: 0 }}
                transition={{
                  opacity: { duration: 0.45, delay: 0.25 + index * 0.08 },
                  y: { duration: 2.4, repeat: Infinity, ease: "easeInOut" },
                  rotateX: { duration: 0.45 },
                }}
              >
                <Icon size={18} className="text-cyan-200" />
                <p className="mt-2 text-sm font-semibold">{card.label}</p>
              </motion.div>
            );
          })}

        <motion.div
          className="relative grid h-52 w-52 place-items-center rounded-[2rem] border border-cyan-300/20 bg-cyan-300/10 shadow-[0_0_90px_rgba(65,192,242,0.28)] backdrop-blur-xl"
          initial={{ opacity: 0, scale: 0.82, rotateY: -25 }}
          animate={{ opacity: 1, scale: 1, rotateY: reducedMotion ? 0 : 360 }}
          transition={{
            opacity: { duration: 0.35 },
            scale: { duration: 0.45, ease: "easeOut" },
            rotateY: { duration: 2.7, ease: "easeInOut" },
          }}
          style={{ transformStyle: "preserve-3d" }}
        >
          <motion.div
            className="absolute inset-4 rounded-[1.5rem] border border-amber-300/25"
            animate={reducedMotion ? undefined : { rotate: 360 }}
            transition={{ duration: 4, repeat: Infinity, ease: "linear" }}
          />
          <div className="relative grid h-28 w-28 place-items-center rounded-3xl bg-white shadow-2xl">
            <img src={tadamunLogo} alt="Tadamun logo" className="h-20 w-20" />
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.45, delay: 0.45 }}
        >
          <p className="mt-8 inline-flex items-center gap-2 rounded-full border border-cyan-300/20 bg-cyan-300/10 px-4 py-2 text-sm font-semibold text-cyan-100">
            <Database size={16} />
            Initializing command center
          </p>
          <h1 className="mt-5 text-4xl font-semibold tracking-normal sm:text-6xl">
            Tadamun CRM
          </h1>
          <p className="mt-4 max-w-xl text-sm leading-6 text-slate-300 sm:text-base">
            Customers, leads, tasks, notes, reporting, and secure team access in one workspace.
          </p>
        </motion.div>

        <motion.div
          className="mt-8 h-1.5 w-full max-w-sm overflow-hidden rounded-full bg-white/10"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.65 }}
        >
          <motion.div
            className="h-full rounded-full bg-gradient-to-r from-cyan-300 via-emerald-300 to-amber-300"
            initial={{ width: "0%" }}
            animate={{ width: "100%" }}
            transition={{ duration: reducedMotion ? 0.35 : 2.6, ease: "easeInOut" }}
          />
        </motion.div>
      </section>
    </main>
  );
}
